/*
 * The MIT License
 *
 * Copyright 2019 .
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * MODELS/DUNE.J3O:
 * Converted from http://quadropolis.us/node/2584 [Public Domain according to the Tags of this Map]
 */

package com.jme3.recast4j.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.recast4j.recast.AreaModification;
import org.recast4j.recast.CompactHeightfield;
import org.recast4j.recast.Context;
import org.recast4j.recast.ContourSet;
import org.recast4j.recast.ConvexVolume;
import org.recast4j.recast.Heightfield;
import org.recast4j.recast.HeightfieldLayerSet;
import org.recast4j.recast.PolyMesh;
import org.recast4j.recast.PolyMeshDetail;
import org.recast4j.recast.Recast;
import org.recast4j.recast.RecastArea;
import org.recast4j.recast.RecastBuilder;
import org.recast4j.recast.RecastBuilder.RecastBuilderProgressListener;
import org.recast4j.recast.RecastBuilderConfig;
import org.recast4j.recast.RecastConfig;
import org.recast4j.recast.RecastConstants;
import org.recast4j.recast.RecastConstants.PartitionType;
import org.recast4j.recast.RecastContour;
import org.recast4j.recast.RecastFilter;
import org.recast4j.recast.RecastLayers;
import org.recast4j.recast.RecastMesh;
import org.recast4j.recast.RecastMeshDetail;
import org.recast4j.recast.RecastRasterization;
import org.recast4j.recast.RecastRegion;
import org.recast4j.recast.geom.ChunkyTriMesh.ChunkyTriMeshNode;
import org.recast4j.recast.geom.InputGeomProvider;
import org.recast4j.recast.geom.TriMesh;

/**
 *
 * @author Robert
 */
public class RecastBuilder2 extends RecastBuilder {
    
    private final RecastBuilderProgressListener progressListener;
    
    public RecastBuilder2() {
        super();
        this.progressListener = null;
    }
    
    public RecastBuilder2(RecastBuilderProgressListener progressListener) {
        super(progressListener);
        this.progressListener = progressListener;
    }
    
    public RecastBuilderResult[][] buildTiles(InputGeomProvider geom, RecastConfig cfg, int threads, 
            List<Integer> listTriLength, List<AreaModification> areaMod) {
        float[] bmin = geom.getMeshBoundsMin();
        float[] bmax = geom.getMeshBoundsMax();
        int[] twh = Recast.calcTileCount(bmin, bmax, cfg.cs, cfg.tileSize);
        int tw = twh[0];
        int th = twh[1];
        RecastBuilderResult[][] result = null;
        if (threads == 1) {
            result = buildSingleThread(geom, cfg, bmin, bmax, tw, th, listTriLength, areaMod);
        } else {
            result = buildMultiThread(geom, cfg, bmin, bmax, tw, th, threads, listTriLength, areaMod);
        }
        return result;
    }
    
    private RecastBuilderResult[][] buildSingleThread(InputGeomProvider geom, RecastConfig cfg, float[] bmin,
            float[] bmax, int tw, int th, List<Integer> listTriLength, List<AreaModification> areaMod) {
        RecastBuilderResult[][] result = new RecastBuilderResult[tw][th];
        AtomicInteger counter = new AtomicInteger();
        for (int x = 0; x < tw; ++x) {
            for (int y = 0; y < th; ++y) {
                result[x][y] = buildTile(geom, cfg, bmin, bmax, x, y, counter, tw * th, listTriLength, areaMod);
            }
        }
        return result;
    }


    private RecastBuilderResult[][] buildMultiThread(InputGeomProvider geom, RecastConfig cfg, float[] bmin,
            float[] bmax, int tw, int th, int threads, List<Integer> listTriLength, List<AreaModification> areaMod) {
        ExecutorService ec = Executors.newFixedThreadPool(threads);
        RecastBuilderResult[][] result = new RecastBuilderResult[tw][th];
        AtomicInteger counter = new AtomicInteger();
        for (int x = 0; x < tw; ++x) {
            for (int y = 0; y < th; ++y) {
                final int tx = x;
                final int ty = y;
                ec.submit((Runnable) () -> {
                    result[tx][ty] = buildTile(geom, cfg, bmin, bmax, tx, ty, counter, tw * th, listTriLength, areaMod);
                });
            }
        }
        ec.shutdown();
        try {
            ec.awaitTermination(1000, TimeUnit.HOURS);
        } catch (InterruptedException e) {
        }
        return result;
    }

    private RecastBuilderResult buildTile(InputGeomProvider geom, RecastConfig cfg, float[] bmin, float[] bmax,
            final int tx, final int ty, AtomicInteger counter, int total, List<Integer> listTriLength, List<AreaModification> areaMod) {
        RecastBuilderResult result = build(geom, new RecastBuilderConfig(cfg, bmin, bmax, tx, ty, true), listTriLength, areaMod);
        if (this.progressListener != null) {
            this.progressListener.onProgress(counter.incrementAndGet(), total);
        }
        return result;
    }
    
    public RecastBuilderResult build(InputGeomProvider geom, RecastBuilderConfig builderCfg, 
            List<Integer> listTriLength, List<AreaModification> areaMod) {

        RecastConfig cfg = builderCfg.cfg;
        Context ctx = new Context();
        Heightfield solid = buildSolidHeightfield(geom, builderCfg, ctx, listTriLength, areaMod);
        CompactHeightfield chf = buildCompactHeightfield(geom, cfg, ctx, solid);

        // Partition the heightfield so that we can use simple algorithm later
        // to triangulate the walkable areas.
        // There are 3 martitioning methods, each with some pros and cons:
        // 1) Watershed partitioning
        // - the classic Recast partitioning
        // - creates the nicest tessellation
        // - usually slowest
        // - partitions the heightfield into nice regions without holes or
        // overlaps
        // - the are some corner cases where this method creates produces holes
        // and overlaps
        // - holes may appear when a small obstacles is close to large open area
        // (triangulation can handle this)
        // - overlaps may occur if you have narrow spiral corridors (i.e
        // stairs), this make triangulation to fail
        // * generally the best choice if you precompute the nacmesh, use this
        // if you have large open areas
        // 2) Monotone partioning
        // - fastest
        // - partitions the heightfield into regions without holes and overlaps
        // (guaranteed)
        // - creates long thin polygons, which sometimes causes paths with
        // detours
        // * use this if you want fast navmesh generation
        // 3) Layer partitoining
        // - quite fast
        // - partitions the heighfield into non-overlapping regions
        // - relies on the triangulation code to cope with holes (thus slower
        // than monotone partitioning)
        // - produces better triangles than monotone partitioning
        // - does not have the corner cases of watershed partitioning
        // - can be slow and create a bit ugly tessellation (still better than
        // monotone)
        // if you have large open areas with small obstacles (not a problem if
        // you use tiles)
        // * good choice to use for tiled navmesh with medium and small sized
        // tiles

        if (cfg.partitionType == PartitionType.WATERSHED) {
            // Prepare for region partitioning, by calculating distance field
            // along the walkable surface.
            RecastRegion.buildDistanceField(ctx, chf);
            // Partition the walkable surface into simple regions without holes.
            RecastRegion.buildRegions(ctx, chf, builderCfg.borderSize, cfg.minRegionArea, cfg.mergeRegionArea);
        } else if (cfg.partitionType == PartitionType.MONOTONE) {
            // Partition the walkable surface into simple regions without holes.
            // Monotone partitioning does not need distancefield.
            RecastRegion.buildRegionsMonotone(ctx, chf, builderCfg.borderSize, cfg.minRegionArea, cfg.mergeRegionArea);
        } else {
            // Partition the walkable surface into simple regions without holes.
            RecastRegion.buildLayerRegions(ctx, chf, builderCfg.borderSize, cfg.minRegionArea);
        }

        //
        // Step 5. Trace and simplify region contours.
        //

        // Create contours.
        ContourSet cset = RecastContour.buildContours(ctx, chf, cfg.maxSimplificationError, cfg.maxEdgeLen,
                RecastConstants.RC_CONTOUR_TESS_WALL_EDGES);

        //
        // Step 6. Build polygons mesh from contours.
        //

        PolyMesh pmesh = RecastMesh.buildPolyMesh(ctx, cset, cfg.maxVertsPerPoly);

        //
        // Step 7. Create detail mesh which allows to access approximate height
        // on each polygon.
        //
        PolyMeshDetail dmesh = builderCfg.buildMeshDetail
                ? RecastMeshDetail.buildPolyMeshDetail(ctx, pmesh, chf, cfg.detailSampleDist, cfg.detailSampleMaxError)
                : null;
        return new RecastBuilderResult(solid, chf, cset, pmesh, dmesh);
    }
    
    private Heightfield buildSolidHeightfield(InputGeomProvider geomProvider, RecastBuilderConfig builderCfg,
            Context ctx, List<Integer> listTriLength, List<AreaModification> areaMod) {
        RecastConfig cfg = builderCfg.cfg;
        //
        // Step 2. Rasterize input polygon soup.
        //

        // Allocate voxel heightfield where we rasterize our input data to.
        Heightfield solid = new Heightfield(builderCfg.width, builderCfg.height, builderCfg.bmin, builderCfg.bmax,
                cfg.cs, cfg.ch);

        // Allocate array that can hold triangle area types.
        // If you have multiple meshes you need to process, allocate
        // and array which can hold the max number of triangles you need to
        // process.

        // Find triangles which are walkable based on their slope and rasterize
        // them.
        // If your input data is multiple meshes, you can transform them here,
        // calculate
        // the are type for each of the meshes and rasterize them.
        for (TriMesh geom : geomProvider.meshes()) {
            float[] verts = geom.getVerts();
            boolean tiled = cfg.tileSize > 0;
            int totaltris = 0;
            if (tiled) {
                float[] tbmin = new float[2];
                float[] tbmax = new float[2];
                tbmin[0] = builderCfg.bmin[0];
                tbmin[1] = builderCfg.bmin[2];
                tbmax[0] = builderCfg.bmax[0];
                tbmax[1] = builderCfg.bmax[2];
                List<ChunkyTriMeshNode> nodes = geom.getChunksOverlappingRect(tbmin, tbmax);
                for (ChunkyTriMeshNode node : nodes) {
                    int[] tris = node.tris;
                    int ntris = tris.length / 3;
                    totaltris += ntris;
                
                //Separate individual triangles into a arrays so we can mark Area Type.
                List<int[]> listTris = new ArrayList<>();
                int fromIndex = 0;
                for(int length: listTriLength) {
                    int[] triangles = new int[length];
                    System.arraycopy(tris, fromIndex, triangles, 0, length);
                    listTris.add(triangles);
                    fromIndex += length;
                }

                /**
                 * Set the Area Type for each triangle. We could use separate cfg 
                 * instead. Would give minor ability to fine tune navMeshes but the 
                 * only benefit would be from cfg.walkableClimb.
                 */
                List<int[]> areas = new ArrayList<>();
                for (int i = 0; i < areaMod.size(); i++) {
                    int[] m_triareas = Recast.markWalkableTriangles(
                            ctx, cfg.walkableSlopeAngle, verts, listTris.get(i), listTris.get(i).length/3, areaMod.get(i));
                    areas.add(m_triareas);
                }

                //Prepare the new array for all areas.
                int[] m_triareasAll = new int[ntris];
                int length = 0;
                //Copy all flagged areas into new array.
                for (int[] area: areas) {
                    System.arraycopy(area, 0, m_triareasAll, length, area.length);
                    length += area.length;
                }                    
                RecastRasterization.rasterizeTriangles(ctx, verts, tris, m_triareasAll, ntris, solid, cfg.walkableClimb);
                }
            } else {
                int[] tris = geom.getTris();
                int ntris = tris.length / 3;
                
                //Separate individual triangles into a arrays so we can mark Area Type.
                List<int[]> listTris = new ArrayList<>();
                int fromIndex = 0;
                for(int length: listTriLength) {
                    int[] triangles = new int[length];
                    System.arraycopy(tris, fromIndex, triangles, 0, length);
                    listTris.add(triangles);
                    fromIndex += length;
                }

                /**
                 * Set the Area Type for each triangle. We could use separate cfg 
                 * instead. Would give minor ability to fine tune navMeshes but the 
                 * only benefit would be from cfg.walkableClimb.
                 */
                List<int[]> areas = new ArrayList<>();
                for (int i = 0; i < areaMod.size(); i++) {
                    int[] m_triareas = Recast.markWalkableTriangles(
                            ctx, cfg.walkableSlopeAngle, verts, listTris.get(i), listTris.get(i).length/3, areaMod.get(i));
                    areas.add(m_triareas);
                }

                //Prepare the new array for all areas.
                int[] m_triareasAll = new int[ntris];
                int length = 0;
                //Copy all flagged areas into new array.
                for (int[] area: areas) {
                    System.arraycopy(area, 0, m_triareasAll, length, area.length);
                    length += area.length;
                }
                
                totaltris = ntris;
                RecastRasterization.rasterizeTriangles(ctx, verts, tris, m_triareasAll, ntris, solid, cfg.walkableClimb);
            }
        }
        //
        // Step 3. Filter walkables surfaces.
        //

        // Once all geometry is rasterized, we do initial pass of filtering to
        // remove unwanted overhangs caused by the conservative rasterization
        // as well as filter spans where the character cannot possibly stand.
        if (cfg.filterLowHangingObstacles) {
            RecastFilter.filterLowHangingWalkableObstacles(ctx, cfg.walkableClimb, solid);
        }
        if (cfg.filterLedgeSpans) {
            RecastFilter.filterLedgeSpans(ctx, cfg.walkableHeight, cfg.walkableClimb, solid);
        }
        if (cfg.filterWalkableLowHeightSpans) {
            RecastFilter.filterWalkableLowHeightSpans(ctx, cfg.walkableHeight, solid);
        }

        return solid;
    }

    private CompactHeightfield buildCompactHeightfield(InputGeomProvider geomProvider, RecastConfig cfg, Context ctx,
            Heightfield solid) {
        //
        // Step 4. Partition walkable surface to simple regions.
        //

        // Compact the heightfield so that it is faster to handle from now on.
        // This will result more cache coherent data as well as the neighbours
        // between walkable cells will be calculated.
        CompactHeightfield chf = Recast.buildCompactHeightfield(ctx, cfg.walkableHeight, cfg.walkableClimb, solid);

        // Erode the walkable area by agent radius.
        RecastArea.erodeWalkableArea(ctx, cfg.walkableRadius, chf);
        // (Optional) Mark areas.
        for (ConvexVolume vol : geomProvider.getConvexVolumes()) {
            RecastArea.markConvexPolyArea(ctx, vol.verts, vol.hmin, vol.hmax, vol.areaMod, chf);
        }
        return chf;
    }

    public HeightfieldLayerSet buildLayers(InputGeomProvider geom, RecastBuilderConfig builderCfg, 
            List<Integer> listTriLength, List<AreaModification> areaMod) {
        Context ctx = new Context();
        Heightfield solid = buildSolidHeightfield(geom, builderCfg, ctx, listTriLength, areaMod);
        CompactHeightfield chf = buildCompactHeightfield(geom, builderCfg.cfg, ctx, solid);
        return RecastLayers.buildHeightfieldLayers(ctx, chf, builderCfg.borderSize, builderCfg.cfg.walkableHeight);
    }

}
