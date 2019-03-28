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
import java.util.Arrays;
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
import org.recast4j.recast.RecastBuilder.RecastBuilderProgressListener;
import org.recast4j.recast.RecastBuilder.RecastBuilderResult;
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
 * Extends the Recast4j RecastBuilder class to allow for Area Type flag setting.
 * 
 * @author Robert
 */
public class RecastBuilder extends org.recast4j.recast.RecastBuilder {
    
    private final RecastBuilderProgressListener progressListener;
    
    public RecastBuilder() {
        super();
        this.progressListener = null;
    }
    
    /**
     * Sets the progress listener for this job. Reports back the completed tile
     * and number of tiles for the job. Setting timers on the callback allows 
     * for accurate determination of build times.
     * 
     * @param progressListener The listener to set for the job.
     */
    public RecastBuilder(RecastBuilderProgressListener progressListener) {
        super(progressListener);
        this.progressListener = progressListener;
    }

    /**
     * Builds the polymesh and detailmesh by creating tiles.
     * 
     * @param geom The geometry to be used for constructing the meshes.
     * @param cfg The configuration parameters to be used for constructing the meshes.
     * @param threads The number of threads to use for this build job.
     * @param listTriLength The listUnmarkedTris of triangle lengths to be used for 
 separating the geometry into areas.
     * @param areaMod The listUnmarkedTris of Area Modifications for each Area Type that is
 derived from the geometry. Expected to be in the same order as the 
 individual geometry to match area modification to triangles in the area.
     * @return The build results.
     */
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
            int[] tris = geom.getTris();
            int ntris = tris.length / 3;
            boolean tiled = cfg.tileSize > 0;
            
            /**
             * Sort triangles into group arrays using the supplied triangle 
             * lengths so we can mark Area Type.
             * 
             * This list will hold each areas triangles as a separate element,
             * with each element matching the order of the Area Types in the 
             * areaMod list.
             */
            List<int[]> listAreaTris = new ArrayList<>();
            int fromIndex = 0;
            for(int length: listTriLength) {
                int[] triangles = new int[length];
                System.arraycopy(tris, fromIndex, triangles, 0, length);
                listAreaTris.add(triangles);
                fromIndex += length;
            }
            
            if (tiled) {
                float[] tbmin = new float[2];
                float[] tbmax = new float[2];
                tbmin[0] = builderCfg.bmin[0];
                tbmin[1] = builderCfg.bmin[2];
                tbmax[0] = builderCfg.bmax[0];
                tbmax[1] = builderCfg.bmax[2];
                
                List<ChunkyTriMeshNode> nodes = geom.getChunksOverlappingRect(tbmin, tbmax);
                for (ChunkyTriMeshNode node : nodes) {
                    int[] node_tris = node.tris;
                    int node_ntris = node_tris.length / 3;
                    
                /**
                 * With listAreaTris we have triangle groups that match the 
                 * areaMod lists Area Modifications order. 
                 * 
                 * We cycle through each nodes triangles, in the same order they 
                 * are found, looking for a matching triangle in one of the 
                 * geometry groups.
                 *
                 * We mark any triangle found immediately and add it to listMarkedTris.
                 * 
                 * Last, we merge all marked triangles into one array for 
                 * Rasterisation.
                 */
                List<Integer> listMarkedTris = new ArrayList<>();
                int[] nodeTri = new int[3];
                int[] areaTri = new int[3];

                for (int i = 0; i < node_ntris; i++) {
                    //Create a triangle from the node.
                    nodeTri[0] = node_tris[i*3];
                    nodeTri[1] = node_tris[i*3+1];
                    nodeTri[2] = node_tris[i*3+2];

                    //Cycle through each area.
                    for (int[] areaTris: listAreaTris) {
                        //If no triangle is found in this group of triangles, 
                        //we will move onto the next group, and the next, 
                        //until we find a match.
                        boolean found = false;

                        //Cycle through each areas triangles.
                        for (int j = 0; j < areaTris.length/3; j++) {

                            //Create triangle from the area.
                            areaTri[0] = areaTris[j*3];
                            areaTri[1] = areaTris[j*3+1];
                            areaTri[2] = areaTris[j*3+2];

                            //If we find a matching triangle in this area, 
                            //we are done with this group.
                            if (Arrays.equals(nodeTri, areaTri)) {
                                found = true;
                                break;
                            }
                        }

                        /**
                         * We found that nodeTri matched areaTri so mark 
                         * Area Type which is represented by its areaTris 
                         * index. This is a single triangle we are passing to 
                         * markWalkableTriangles. We then break out of the loop 
                         * to advance to the next node triangle to check. If no
                         * match was found for this group, we check the next 
                         * area and continue the search. 
                         */
                        if (found) {
                            //Mark single triangle.
                            int[] m_triareas = Recast.markWalkableTriangles(
                                    ctx, cfg.walkableSlopeAngle, verts, nodeTri, nodeTri.length/3,areaMod.get(listAreaTris.indexOf(areaTris)));
                            
                            /**
                             * Add marked triangle area to the listMarkedTris.
                             * We passed in a single triangle so there is only
                             * one element in the array.
                             */
                            listMarkedTris.add(m_triareas[0]);
                            break;
                        }
                    }

                }

                //Prepare a new array to combine all marked triangles.
                int[] mergeArea = new int[node_ntris];
                //Copy each marked triangle into the new array.
                for (int i = 0; i < mergeArea.length; i++) {
                    mergeArea[i] = listMarkedTris.get(i);
                }   
                                        
                RecastRasterization.rasterizeTriangles(ctx, verts, node_tris, mergeArea, node_ntris, solid, cfg.walkableClimb);
                    //========== TESTING STUFF ==========
                    /**
                     * Test each area types merged array for perfect match. The
                     * control group is Recast4j normal build method where there
                     * is only one mesh and all Area Types are set to one. You 
                     * can compare the control group alignment to the other two 
                     * outputs to see if the changed area types are identical. 
                     * 
                     * Tests that run perfect will match the changes of the 
                     * control group where all zeros will be same for all groups
                     * with the only difference being the flags that are changed 
                     * for multiple mesh builds.
                     * 
                     * The test sends a fail message to the log for any area that 
                     * fails to match, excluding the control group obviously. 
                     * 
                     * You can search the log output for the "TEST FAILED" 
                     * message to quickly identify fails. There should never be 
                     * one.
                     */
//                    if (!Arrays.equals(mergeArea, testAreaTypes(ctx, cfg, verts, node_tris, listAreaTris, areaMod ))) {
//                        System.out.println("TEST FAILED");
//                    }
//                    
//                    System.out.println("flagWhenFound " + Arrays.toString(mergeArea) 
//                            + " length " + mergeArea.length + "\n");
                }
            } else {

                /**
                 * Set the Area Type for each triangle. We could use separate cfg 
                 * instead. Would give minor ability to fine tune navMeshes but the 
                 * only benefit would be from cfg.walkableClimb.
                 */
                List<int[]> listMarkedTris = new ArrayList<>();
                for (int i = 0; i < areaMod.size(); i++) {
                    int[] m_triareas = Recast.markWalkableTriangles(ctx, cfg.walkableSlopeAngle, verts, listAreaTris.get(i), listAreaTris.get(i).length/3, areaMod.get(i));
                    listMarkedTris.add(m_triareas);
                }

                //Prepare a new array to combine all marked triangles.
                int[] mergeTriArea = new int[ntris];
                int length = 0;
                //Copy each marked triangle into the new array.
                for (int[] area: listMarkedTris) {
                    System.arraycopy(area, 0, mergeTriArea, length, area.length);
                    length += area.length;
                }
                
//                System.out.println("mergeArea " + Arrays.toString(mergeArea) + " length " + mergeArea.length + "\n");
                
                RecastRasterization.rasterizeTriangles(ctx, verts, tris, mergeTriArea, ntris, solid, cfg.walkableClimb);
                
//                int[] controlGroup = Recast.markWalkableTriangles(ctx, cfg.walkableSlopeAngle, verts, tris, ntris,cfg.walkableAreaMod);
//                System.out.println("controlGroup " + Arrays.toString(controlGroup) + " length " + controlGroup.length + "\n");
                
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
    
    /**
     * Test that gathers Area Types into arrays before marking Area Types. If
     * this loop can be improved, it may be a better solution than marking the 
     * triangles immediately when found, as is done in the buildSolidHeightfield
     * method, since there is an unknown factor for mountain type terrains or 
     * when there are lots of meshes to flag.
     * 
     * @param ctx Context for error messages.
     * @param cfg The configuration used for this test.
     * @param verts The vertices for the test.
     * @param node_tris The triangles for this test.
     * @param listAreaTris The area triangles.
     * @param areaMod The area modifications.
     * @return An array of Area Types.
     */
    private int[] testAreaTypes(Context ctx, RecastConfig cfg, float[] verts, 
            int[] node_tris, List<int[]> listAreaTris, List<AreaModification> areaMod) {
        
        int node_ntris = node_tris.length / 3;
        
        /**
         * With listAreaTris we have triangle groups that match the areaMod 
         * lists Area Modifications order. 
         * 
         * A node triangle can be pulled from any piece of neighboring geometry 
         * and these found triangles can be in any listAreaTris group, and in no 
         * certain order.
         * 
         * We cycle through each nodes triangles, in the same order they are 
         * found, looking for a matching triangle in one of the geometry groups.
         * 
         * We gather all triangles found in the same geometry into one array, 
         * marking the Area Type for each array, whenever the listAreaTris 
         * changes indexes or if we reach the last triangle in the node. 
         * 
         * We mark the triangles when the index changes for listAreaTris because 
         * this indicates that geometry did not contain the triangle and the 
         * next geometry may be a different Area Type but we may have unmarked 
         * triangles in listUnmarkedTris.
         * 
         * We mark the triangles when reaching the end of the nodes triangles 
         * because its possible that all the nodes triangles are in the same 
         * geometry, which means you would not be changing the index of 
         * listAreaTris.
         */
        List<int[]> listMarkedTris = new ArrayList<>();
        //Assure at least one pass of search.
        do {

            //Where we are in the node array.
            int nodeIndex = 0;
            //Previous area.
            int prevArea = 0;
            //Current area.
            int indexOfArea = 0;

            int[] nodeTri = new int[3];
            int[] areaTri = new int[3];
            boolean found;
            List<Integer> listUnmarkedTris = new ArrayList<>();

            //Node triangles.
            for (int i = 0; i < node_ntris; i++) {                            
                //We need this to know when we are at the end of this nodes 
                //triangles so we can exit.
                nodeIndex = i;

                //Create a triangle from the node.
                nodeTri[0] = node_tris[i*3];
                nodeTri[1] = node_tris[i*3+1];
                nodeTri[2] = node_tris[i*3+2];   

                /**
                 * Search will start from first geometry and continue until we 
                 * find the triangle.
                 */
                found = false;

                while(true) {

                    //Area triangles.
                    for(int[] areaTris: listAreaTris) {

                        //Track current area type.
                        indexOfArea = listAreaTris.indexOf(areaTris);

                        for (int j = 0; j < areaTris.length/3; j++) {

                            //Create triangle from the area.
                            areaTri[0] = areaTris[j*3];
                            areaTri[1] = areaTris[j*3+1];
                            areaTri[2] = areaTris[j*3+2];

                            /**
                             * If we find a matching triangle in this area, we 
                             * are done with this group.
                             */
                            if (Arrays.equals(nodeTri, areaTri)) {

                                //Notify the outer loops we are done. 
                                found = true;

                                /**
                                 * If the list still has elements and we are 
                                 * here it means there is a check of the next 
                                 * geometry going on and it may not be the same 
                                 * area type so we need to mark these triangles 
                                 * and clear the list for any new triangles that 
                                 * may from come from this current geometry.
                                 */
                                if (listUnmarkedTris.size() > 0 && indexOfArea != prevArea) {

                                    //Move unmarked tris into array.
                                    int[] array = new int[listUnmarkedTris.size()];
                                    for (int idx = 0; idx < array.length; idx++) {
                                        array[idx] = listUnmarkedTris.get(idx);
                                    }

//                                    System.out.println("MARK AREA "  + prevArea 
//                                            + " area mod " + areaMod.get(prevArea).getMaskedValue());

                                    /**
                                     * Mark the Area Type based on previous area 
                                     * since we have a new triangle with possibly  
                                     * different area type.
                                     */
                                    int[] m_triareas = Recast.markWalkableTriangles(
                                            ctx, cfg.walkableSlopeAngle, verts, array, array.length/3,areaMod.get(prevArea));

                                    //Add to the marked list.
                                    listMarkedTris.add(m_triareas);

                                    //Clear the unmarked list.
                                    listUnmarkedTris.clear();
                                }

//                                System.out.println("FOUND nodeIndex " + nodeIndex 
//                                        + " areaIndex " + j 
//                                        +  " area " + listAreaTris.indexOf(areaTris) 
//                                        + " area mod " + areaMod.get(listAreaTris.indexOf(areaTris)).getMaskedValue());

                                //Add found triangle to the unmarked list.
                                listUnmarkedTris.add(nodeTri[0]);
                                listUnmarkedTris.add(nodeTri[1]);
                                listUnmarkedTris.add(nodeTri[2]);

                                //Track area changes.
                                if (indexOfArea != prevArea) {
                                    prevArea = indexOfArea;
                                }
                                break;
                            }                                        
                        }

                        /**
                         * If we are here, we have reached the end 
                         * of this nodes triangles so we need to 
                         * mark unmarked triangles. We need to know 
                         * the Area Type so do it here.
                         */
                        if (listUnmarkedTris.size() > 0 && nodeIndex == node_ntris - 1) {

                            //Move unmarked tris into array.
                            int[] array = new int[listUnmarkedTris.size()];
                            for (int idx = 0; idx < array.length; idx++) {
                                array[idx] = listUnmarkedTris.get(idx);
                            }

//                            System.out.println("MARK AREA " + listAreaTris.indexOf(areaTris) 
//                                    + " area mod " + areaMod.get(listAreaTris.indexOf(areaTris)).getMaskedValue());

                            /**
                             * Mark the Area Type based on current area since we 
                             * have reached the end of the nodes triangles.
                             */
                            int[] m_triareas = Recast.markWalkableTriangles(
                                    ctx, cfg.walkableSlopeAngle, verts, array, array.length/3,areaMod.get(listAreaTris.indexOf(areaTris)));
                            
                            //Add to the marked list.
                            listMarkedTris.add(m_triareas);
                            
                            //Clear the unmarked list.
                            listUnmarkedTris.clear();
                        }

                        //If found, we are done with this node tri.
                        if (found) {     
                            break;
                        }
                    }

                    /**
                     * If found, we are done with this node tri. By breaking out 
                     * we will either advance to the  next tri or exit marking.
                     */
                    if (found) {
                        break;
                    }
                }
            }

            //If we are here, we have reached the end of this nodes 
            //triangles so exit.
            if (nodeIndex == node_ntris - 1) {
                break;
            }
        } while(true);

        //Prepare a new array to combine all marked triangles.
        int[] mergeArea = new int[node_ntris];
        int lengths = 0;
        //Copy each marked triangle into the new array.
        for (int[] area: listMarkedTris) {
            System.arraycopy(area, 0, mergeArea, lengths, area.length);
            lengths += area.length;
        }


        System.out.println("flagUseArrays " + Arrays.toString(mergeArea) + " length " + mergeArea.length + "\n");
        
        int[] controlGroup = Recast.markWalkableTriangles(ctx, cfg.walkableSlopeAngle, verts, node_tris, node_ntris,cfg.walkableAreaMod);
        
        System.out.println("Control_Group " + Arrays.toString(controlGroup) + " length " + controlGroup.length + "\n");
        
        return mergeArea;
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
