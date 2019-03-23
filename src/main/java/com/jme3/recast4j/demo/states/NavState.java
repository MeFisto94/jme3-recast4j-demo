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

package com.jme3.recast4j.demo.states;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.collision.CollisionResults;
import com.jme3.input.MouseInput;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.recast4j.Detour.BetterDefaultQueryFilter;
import com.jme3.recast4j.Detour.DetourUtils;
import com.jme3.recast4j.Recast.GeometryProviderBuilder;
import com.jme3.recast4j.Recast.NavMeshDataCreateParamsBuilder;
import com.jme3.recast4j.Recast.RecastBuilderConfigBuilder;
import com.jme3.recast4j.Recast.RecastConfigBuilder;
import com.jme3.recast4j.Recast.RecastUtils;
import com.jme3.recast4j.Recast.SampleAreaModifications;
import static com.jme3.recast4j.Recast.SampleAreaModifications.SAMPLE_AREAMOD_DOOR;
import static com.jme3.recast4j.Recast.SampleAreaModifications.SAMPLE_AREAMOD_GRASS;
import static com.jme3.recast4j.Recast.SampleAreaModifications.SAMPLE_AREAMOD_JUMP;
import static com.jme3.recast4j.Recast.SampleAreaModifications.SAMPLE_AREAMOD_ROAD;
import static com.jme3.recast4j.Recast.SampleAreaModifications.SAMPLE_AREAMOD_WATER;
import com.jme3.recast4j.demo.RecastBuilder2;
import com.jme3.recast4j.demo.controls.PhysicsAgentControl;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitor;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Line;
import com.simsilica.lemur.event.DefaultMouseListener;
import com.simsilica.lemur.event.MouseEventControl;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.recast4j.detour.FindNearestPolyResult;
import org.recast4j.detour.MeshData;
import org.recast4j.detour.NavMesh;
import org.recast4j.detour.NavMeshBuilder;
import org.recast4j.detour.NavMeshDataCreateParams;
import org.recast4j.detour.NavMeshParams;
import org.recast4j.detour.NavMeshQuery;
import org.recast4j.detour.QueryFilter;
import org.recast4j.detour.Result;
import org.recast4j.detour.Status;
import org.recast4j.detour.StraightPathItem;
import org.recast4j.detour.io.MeshDataWriter;
import org.recast4j.detour.io.MeshSetReader;
import org.recast4j.detour.io.MeshSetWriter;
import org.recast4j.recast.AreaModification;
import org.recast4j.recast.CompactHeightfield;
import org.recast4j.recast.Context;
import org.recast4j.recast.ContourSet;
import org.recast4j.recast.Heightfield;
import org.recast4j.recast.PolyMesh;
import org.recast4j.recast.PolyMeshDetail;
import org.recast4j.recast.Recast;
import org.recast4j.recast.RecastArea;
import org.recast4j.recast.RecastBuilder;
import org.recast4j.recast.RecastBuilder.RecastBuilderProgressListener;
import org.recast4j.recast.RecastBuilder.RecastBuilderResult;
import org.recast4j.recast.RecastBuilderConfig;
import org.recast4j.recast.RecastConfig;
import org.recast4j.recast.RecastConstants;
import org.recast4j.recast.RecastConstants.PartitionType;
import org.recast4j.recast.RecastContour;
import org.recast4j.recast.RecastFilter;
import org.recast4j.recast.RecastMesh;
import org.recast4j.recast.RecastMeshDetail;
import org.recast4j.recast.RecastRasterization;
import org.recast4j.recast.RecastRegion;
import org.recast4j.recast.geom.InputGeomProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.recast4j.recast.geom.TriMesh;
import static org.recast4j.recast.RecastVectors.copy;

/**
 *
 * @author Robert
 */
public class NavState extends BaseAppState {

    private static final Logger LOG = LoggerFactory.getLogger(NavState.class.getName());
    
    private Node worldMap;
    private NavMesh navMesh;
    private NavMeshQuery query;
    private List<Node> characters;
    private List<Geometry> pathGeometries;

    public NavState() {
        pathGeometries = new ArrayList<>(64);
        characters = new ArrayList<>(64);  
    }
    
    @Override
    protected void initialize(Application app) {
    }

    @Override
    protected void cleanup(Application app) {
        //TODO: clean up what you initialized in the initialize method,
        //e.g. remove all spatials from rootNode
    }

    //onEnable()/onDisable() can be used for managing things that should 
    //only exist while the state is enabled. Prime examples would be scene 
    //graph attachment or input listener attachment.
    @Override
    protected void onEnable() {
        worldMap = (Node) ((SimpleApplication) getApplication()).getRootNode().getChild("worldmap");
//        buildSolo();
//        buildTiled();
//        buildSoloRecast4j();
        buildSoloTest();
        
        MouseEventControl.addListenersToSpatial(worldMap, new DefaultMouseListener() {
            @Override
            protected void click(MouseButtonEvent event, Spatial target, Spatial capture) {
                super.click(event, target, capture);
                
                // First clear existing pathGeometries from the old path finding:
                pathGeometries.forEach(Geometry::removeFromParent);
                // Clicked on the map, so params a path to:
                Vector3f locOnMap = getLocationOnMap(); // Don'from calculate three times
                LOG.info("Will walk from {} to {}", getCharacters().get(0).getWorldTranslation(), locOnMap);
                ((SimpleApplication) getApplication()).getRootNode().attachChild(placeColoredBoxAt(ColorRGBA.Green, getCharacters().get(0).getWorldTranslation().add(0f, 0.5f, 0f)));
                ((SimpleApplication) getApplication()).getRootNode().attachChild(placeColoredBoxAt(ColorRGBA.Yellow, locOnMap.add(0f, 0.5f, 0f)));
                
                if (getCharacters().size() == 1) {
                    QueryFilter filter = new BetterDefaultQueryFilter();
                    FindNearestPolyResult startPoly = query.findNearestPoly(getCharacters().get(0).getWorldTranslation().toArray(null), new float[]{1.0f, 1.0f, 1.0f}, filter);
                    FindNearestPolyResult endPoly = query.findNearestPoly(DetourUtils.toFloatArray(locOnMap), new float[]{1.0f, 1.0f, 1.0f}, filter);
                    if (startPoly.getNearestRef() == 0 || endPoly.getNearestRef() == 0) {
                        LOG.info("Neither Start or End reference can be 0. startPoly [{}] endPoly [{}]", startPoly, endPoly);
                        pathGeometries.forEach(Geometry::removeFromParent);
                    } else {
                        if (event.getButtonIndex() == MouseInput.BUTTON_LEFT) {
                            findPathImmediately(getCharacters().get(0), filter, startPoly, endPoly);
                        } else if (event.getButtonIndex() == MouseInput.BUTTON_RIGHT) {
                            findPathSlicedPartial(getCharacters().get(0), filter, startPoly, endPoly);
                        }
                    }
                }
            }
        });
    }
    
    private void findPathImmediately(Node character, QueryFilter filter, FindNearestPolyResult startPoly, FindNearestPolyResult endPoly) {
        Result<List<Long>> fpr = query.findPath(startPoly.getNearestRef(), endPoly.getNearestRef(), startPoly.getNearestPos(), endPoly.getNearestPos(), filter);
        if (fpr.succeeded()) {
            // Get the proper path from the rough polygon listing
            Result<List<StraightPathItem>> list = query.findStraightPath(startPoly.getNearestPos(), endPoly.getNearestPos(), fpr.result, 256, 0);
            Vector3f oldPos = character.getWorldTranslation();
            List<Vector3f> vector3fList = new ArrayList<>(list.result.size());
            if (!list.result.isEmpty()) {
                for (StraightPathItem p: list.result) {
                    Vector3f nu = DetourUtils.createVector3f(p.getPos());
                    ((SimpleApplication) getApplication()).getRootNode().attachChild(placeColoredLineBetween(ColorRGBA.Orange, oldPos.add(0f, 0.5f, 0f), nu.add(0f, 0.5f, 0f)));
                    if (p.getRef() != 0) { // if ref is 0, it's the end.
                        ((SimpleApplication) getApplication()).getRootNode().attachChild(placeColoredBoxAt(ColorRGBA.Blue, nu.add(0f, 0.5f, 0f)));
                    }
                    vector3fList.add(nu);
                    oldPos = nu;
                }

                character.getControl(PhysicsAgentControl.class).stopFollowing();
                character.getControl(PhysicsAgentControl.class).followPath(vector3fList);
            } else {
                System.err.println("Unable to find straight paths");
            }
        } else {
            System.err.println("I'm sorry, unable to find a path.....");
        }
    }

    private void findPathSliced(Node character, QueryFilter filter, FindNearestPolyResult startPoly, FindNearestPolyResult endPoly) {
        query.initSlicedFindPath(startPoly.getNearestRef(), endPoly.getNearestRef(), startPoly.getNearestPos(), endPoly.getNearestPos(), filter, 0);

        Result<Integer> res;
        do {
            // typically called from a control or appstate, so simulate it with a loop and sleep.
            res = query.updateSlicedFindPath(1);
            try {
                Thread.sleep(10);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        } while (res.status == Status.IN_PROGRESS);

        Result<List<Long>> fpr = query.finalizeSlicedFindPath();

        // @TODO: Use NavMeshSliceControl (but then how to do the Debug Graphics?)
        // @TODO: Try Partial. How would one make this logic with controls etc so it's easy?
        //query.finalizeSlicedFindPathPartial();

        if (fpr.succeeded()) {
            // Get the proper path from the rough polygon listing
            Result<List<StraightPathItem>> list = query.findStraightPath(startPoly.getNearestPos(), endPoly.getNearestPos(), fpr.result, Integer.MAX_VALUE, 0);
            Vector3f oldPos = character.getWorldTranslation();
            List<Vector3f> vector3fList = new ArrayList<>(list.result.size());

            if (!list.result.isEmpty()) {
                for (StraightPathItem p: list.result) {
                    Vector3f nu = DetourUtils.createVector3f(p.getPos());
                    ((SimpleApplication) getApplication()).getRootNode().attachChild(placeColoredLineBetween(ColorRGBA.Orange, oldPos.add(0f, 0.5f, 0f), nu.add(0f, 0.5f, 0f)));
                    if (p.getRef() != 0) { // if ref is 0, it's the end.
                        ((SimpleApplication) getApplication()).getRootNode().attachChild(placeColoredBoxAt(ColorRGBA.Blue, nu.add(0f, 0.5f, 0f)));
                    }
                    vector3fList.add(nu);
                    oldPos = nu;
                }

                character.getControl(PhysicsAgentControl.class).stopFollowing();
                character.getControl(PhysicsAgentControl.class).followPath(vector3fList);
            } else {
                System.err.println("Unable to find straight paths");
            }
        } else {
            System.err.println("I'm sorry, unable to find a path.....");
        }
    }

    // Partial means canceling before being finished
    private void findPathSlicedPartial(Node character, QueryFilter filter, FindNearestPolyResult startPoly, FindNearestPolyResult endPoly) {
        query.initSlicedFindPath(startPoly.getNearestRef(), endPoly.getNearestRef(), startPoly.getNearestPos(),
                endPoly.getNearestPos(), filter, 0);
        Result<Integer> res;
        res = query.updateSlicedFindPath(1);
        Result<List<Long>> fpr = query.finalizeSlicedFindPath();

        query.initSlicedFindPath(startPoly.getNearestRef(), endPoly.getNearestRef(), startPoly.getNearestPos(),
                endPoly.getNearestPos(), filter, 0);
        Result<List<Long>> fpr2 = query.finalizeSlicedFindPathPartial(fpr.result);

        // @TODO: Use NavMeshSliceControl (but then how to do the Debug Graphics?)
        // @TODO: Try Partial. How would one make this logic with controls etc so it's easy?
        //query.finalizeSlicedFindPathPartial();

        if (fpr2.succeeded()) {
            // Get the proper path from the rough polygon listing
            Result<List<StraightPathItem>> list = query.findStraightPath(startPoly.getNearestPos(), endPoly.getNearestPos(), fpr2.result, Integer.MAX_VALUE, 0);
            Vector3f oldPos = character.getWorldTranslation();
            List<Vector3f> vector3fList = new ArrayList<>(list.result.size());

            if (!list.result.isEmpty()) {
                for (StraightPathItem p: list.result) {
                    Vector3f nu = DetourUtils.createVector3f(p.getPos());
                    ((SimpleApplication) getApplication()).getRootNode().attachChild(placeColoredLineBetween(ColorRGBA.Orange, oldPos.add(0f, 0.5f, 0f), nu.add(0f, 0.5f, 0f)));
                    if (p.getRef() != 0) { // if ref is 0, it's the end.
                        ((SimpleApplication) getApplication()).getRootNode().attachChild(placeColoredBoxAt(ColorRGBA.Blue, nu.add(0f, 0.5f, 0f)));
                    }
                    vector3fList.add(nu);
                    oldPos = nu;
                }

                character.getControl(PhysicsAgentControl.class).stopFollowing();
                character.getControl(PhysicsAgentControl.class).followPath(vector3fList);
            } else {
                System.err.println("Unable to find straight paths");
            }
        } else {
            System.err.println("I'm sorry, unable to find a path.....");
        }
    }
    
    private void showDebugMeshes(MeshData meshData, boolean wireframe) {
        Material matRed = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        matRed.setColor("Color", ColorRGBA.Red);
        
        if (wireframe) {
            matRed.getAdditionalRenderState().setWireframe(true);
        }

        Material matGreen = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        matGreen.setColor("Color", ColorRGBA.Green);

        // navMesh.getTile(0).data == meshData (in this particular case)
        Geometry gDetailed = new Geometry("DebugMeshDetailed", RecastUtils.getDebugMesh(meshData.detailMeshes, meshData.detailVerts, meshData.detailTris));
        Geometry g = new Geometry("DebugMeshSimple", RecastUtils.getDebugMesh(meshData));
        g.setMaterial(matRed);
        gDetailed.setMaterial(matGreen);
        System.out.println("VertCount Regular Mesh: " + g.getVertexCount());
        System.out.println("VertCount Detailed Mesh: " + gDetailed.getVertexCount());
        g.move(0f, 0.125f, 0f);
        gDetailed.move(0f, 0.25f, 0f);

        ((SimpleApplication) getApplication()).getRootNode().attachChild(g);
        ((SimpleApplication) getApplication()).getRootNode().attachChild(gDetailed);
    }
    
    private void buildSolo() {
        System.out.println("Building Nav Mesh, this may freeze your computer for a few seconds, please stand by");
        long time = System.currentTimeMillis(); // Never do real benchmarking with currentTimeMillis!
        RecastBuilderConfig bcfg = new RecastBuilderConfigBuilder(worldMap).
                build(new RecastConfigBuilder()
                        .withAgentRadius(.3f)           // r
                        .withAgentHeight(1.7f)          // h
                        //cs and ch should probably be .1 at min.
                        .withCellSize(.1f)              // cs=r/3
                        .withCellHeight(.1f)            // ch=cs 
                        .withAgentMaxClimb(.3f)         // > 2*ch
                        .withAgentMaxSlope(45f)         
                        .withEdgeMaxLen(2.4f)             // r*8
                        .withEdgeMaxError(1.3f)         // 1.1 - 1.5
                        .withDetailSampleDistance(8.0f) // increase if exception
                        .withDetailSampleMaxError(8.0f) // increase if exception
                        .withVertsPerPoly(3).build());
        
        //Split up for testing.
        NavMeshDataCreateParams build = new NavMeshDataCreateParamsBuilder(
                new RecastBuilder().build(new GeometryProviderBuilder(worldMap).build(), bcfg)).build(bcfg);
        MeshData meshData = NavMeshBuilder.createNavMeshData(build);
        navMesh = new NavMesh(meshData, bcfg.cfg.maxVertsPerPoly, 0);
        query = new NavMeshQuery(navMesh);
        
        try {
            MeshDataWriter mdw = new MeshDataWriter();
            mdw.write(new FileOutputStream(new File("test.md")),  meshData, ByteOrder.BIG_ENDIAN, false);
            MeshSetWriter msw = new MeshSetWriter();
            msw.write(new FileOutputStream(new File("test.nm")), navMesh, ByteOrder.BIG_ENDIAN, false);
        } catch (Exception ex) {
            LOG.error("[{}]", ex);
        }

        //Show wireframe. Helps with param tweaks. false = solid color.
        showDebugMeshes(meshData, true);
        
        System.out.println("Building succeeded after " + (System.currentTimeMillis() - time) + " ms");
    }
    
    private PartitionType m_partitionType = PartitionType.WATERSHED;
    
    /**
     * Returns the first string after split "_" converted to lower case. 
     * Expected use is for material names.
     * 
     * Example: 
     *      road_asphalt = road
     *      water_blue   = water
     *      grass_green  = grass
     * 
     * @param str Material name to parse. 
     * @return First returned string of split converted to lower case.
     */
    private String getModification(String str) {
        String[] split = str.toLowerCase().split("_");
        return split[0];
    }
    
    private float maxClimb = .3f;
    private float radius = 0.4f;
    private float height = 2.0f;
    private float cs = 0.2f;
    private float ch = 0.1f;
    
    private void buildSoloRecast4j() {
        
        List<Integer> listTriLength = new ArrayList<>();
        List<AreaModification> areaMod = new ArrayList<>();
       
        SceneGraphVisitor visitor = new SceneGraphVisitor() {

            @Override
            public void visit(Spatial spat) {
                if (spat instanceof Geometry) {
                    //Load triangle lengths so we can pick them out from the 
                    //TriMesh later.
                    listTriLength.add(getTriangles(((Geometry) spat).getMesh()).length);
                    
                    /**
                     * Set Are Type based off materials in this case. UserData 
                     * can be added as a optional way to do this. UserData would 
                     * require separating the geometry in blender which is not 
                     * any different really than using materials. 
                     * 
                     * Doors could work the same way, mark the path between the 
                     * two rooms with a material or separate the door path 
                     * geometry into a separate object so it can be picked out. 
                     * 
                     * Off mesh connections can use a similar format. We could 
                     * parse the geometry looking for two connection geometry 
                     * that are flagged as same connection and set the off mesh 
                     * connections programmatically. 
                     */
                    String name = getModification(((Geometry) spat).getMaterial().getName());
                    System.out.println(name);
                    
                    switch (name) {
                        
                        case "water":
                            areaMod.add(SAMPLE_AREAMOD_WATER);
                            break;
                        case "road":
                            areaMod.add(SAMPLE_AREAMOD_ROAD);
                            break;
                        case "grass":
                            areaMod.add(SAMPLE_AREAMOD_GRASS);
                            break;
                        case "door":
                            areaMod.add(SAMPLE_AREAMOD_DOOR);
                            break;
                        case "jump":
                            areaMod.add(SAMPLE_AREAMOD_JUMP);
                            break;
                        default:
                            areaMod.add(SampleAreaModifications.SAMPLE_AREAMOD_GROUND);
                    }
                }
            }
        };
        
        ((SimpleApplication) getApplication()).getRootNode().getChild("worldmap").depthFirstTraversal(visitor);

        //Build merged mesh.
        InputGeomProvider geomProvider = new GeometryProviderBuilder(
                (Node)((SimpleApplication) getApplication()).getRootNode().getChild("worldmap")).build();

        //Get min/max bounds.
        float[] bmin = geomProvider.getMeshBoundsMin();
        float[] bmax = geomProvider.getMeshBoundsMax();
        Context m_ctx = new Context();
        
        //We could use multiple configs here based off area type list.
        RecastConfigBuilder builder = new RecastConfigBuilder();
        RecastConfig cfg = builder
            .withAgentRadius(radius)              // r
            .withAgentHeight(height)              // h
            //cs and ch should be .1 at min.
            .withCellSize(cs)                 // cs=r/2
            .withCellHeight(ch)               // ch=cs/2 but not < .1f
            .withAgentMaxClimb(maxClimb)            // > 2*ch
            .withAgentMaxSlope(45f)
            .withEdgeMaxLen(3.2f)               // r*8
            .withEdgeMaxError(1.3f)             // 1.1 - 1.5
            .withDetailSampleDistance(8.0f)     // increase if exception
            .withDetailSampleMaxError(8.0f)     // increase if exception
            .withWalkableAreaMod(SampleAreaModifications.SAMPLE_AREAMOD_GROUND)
            .withVertsPerPoly(3).build();
        
        RecastBuilderConfig bcfg = new RecastBuilderConfig(cfg, bmin, bmax);
        
        Heightfield m_solid = new Heightfield(bcfg.width, bcfg.height, bcfg.bmin, bcfg.bmax, cfg.cs, cfg.ch);
        for (TriMesh geom : geomProvider.meshes()) {
            float[] verts = geom.getVerts();
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
                        m_ctx, cfg.walkableSlopeAngle, verts, listTris.get(i), listTris.get(i).length/3, areaMod.get(i));
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
            RecastRasterization.rasterizeTriangles(m_ctx, verts, tris, m_triareasAll, ntris, m_solid, cfg.walkableClimb);
        }
        
        RecastFilter.filterLowHangingWalkableObstacles(m_ctx, cfg.walkableClimb, m_solid);
        RecastFilter.filterLedgeSpans(m_ctx, cfg.walkableHeight, cfg.walkableClimb, m_solid);
        RecastFilter.filterWalkableLowHeightSpans(m_ctx, cfg.walkableHeight, m_solid);

        CompactHeightfield m_chf = Recast.buildCompactHeightfield(m_ctx, cfg.walkableHeight, cfg.walkableClimb,
                m_solid);

        RecastArea.erodeWalkableArea(m_ctx, cfg.walkableRadius, m_chf);

        /*
         * ConvexVolume vols = m_geom->getConvexVolumes(); for (int i = 0; i < m_geom->getConvexVolumeCount(); ++i)
         * rcMarkConvexPolyArea(m_ctx, vols[i].verts, vols[i].nverts, vols[i].hmin, vols[i].hmax, (unsigned
         * char)vols[i].area, *m_chf);
         */

        if (m_partitionType == PartitionType.WATERSHED) {
            // Prepare for region partitioning, by calculating distance field
            // along the walkable surface.
            RecastRegion.buildDistanceField(m_ctx, m_chf);
            // Partition the walkable surface into simple regions without holes.
            RecastRegion.buildRegions(m_ctx, m_chf, 0, cfg.minRegionArea, cfg.mergeRegionArea);
        } else if (m_partitionType == PartitionType.MONOTONE) {
            // Partition the walkable surface into simple regions without holes.
            // Monotone partitioning does not need distancefield.
            RecastRegion.buildRegionsMonotone(m_ctx, m_chf, 0, cfg.minRegionArea, cfg.mergeRegionArea);
        } else {
            // Partition the walkable surface into simple regions without holes.
            RecastRegion.buildLayerRegions(m_ctx, m_chf, 0, cfg.minRegionArea);
        }

        ContourSet m_cset = RecastContour.buildContours(m_ctx, m_chf, cfg.maxSimplificationError, cfg.maxEdgeLen,
                RecastConstants.RC_CONTOUR_TESS_WALL_EDGES);

        // Build polygon navmesh from the contours.
        PolyMesh m_pmesh = RecastMesh.buildPolyMesh(m_ctx, m_cset, cfg.maxVertsPerPoly);

        //Set Ability flags.
        for (int i = 0; i < m_pmesh.npolys; ++i) {
            if (m_pmesh.areas[i] == SampleAreaModifications.SAMPLE_POLYAREA_TYPE_GROUND
              || m_pmesh.areas[i] == SampleAreaModifications.SAMPLE_POLYAREA_TYPE_GRASS
              || m_pmesh.areas[i] == SampleAreaModifications.SAMPLE_POLYAREA_TYPE_ROAD) {
                m_pmesh.flags[i] = SampleAreaModifications.SAMPLE_POLYFLAGS_WALK;
            } else if (m_pmesh.areas[i] == SampleAreaModifications.SAMPLE_POLYAREA_TYPE_WATER) {
                m_pmesh.flags[i] = SampleAreaModifications.SAMPLE_POLYFLAGS_SWIM;
            } else if (m_pmesh.areas[i] == SampleAreaModifications.SAMPLE_POLYAREA_TYPE_DOOR) {
                m_pmesh.flags[i] = SampleAreaModifications.SAMPLE_POLYFLAGS_WALK
                | SampleAreaModifications.SAMPLE_POLYFLAGS_DOOR;
            }
            if (m_pmesh.areas[i] > 0) {
                m_pmesh.areas[i]--;
            }
        }

        //Create detailed mesh for picking.
        PolyMeshDetail m_dmesh = RecastMeshDetail.buildPolyMeshDetail(m_ctx, m_pmesh, m_chf, cfg.detailSampleDist,
                cfg.detailSampleMaxError);
        
        NavMeshDataCreateParams params = new NavMeshDataCreateParams();

        params.verts = m_pmesh.verts;
        params.vertCount = m_pmesh.nverts;
        params.polys = m_pmesh.polys;
        params.polyAreas = m_pmesh.areas;
        params.polyFlags = m_pmesh.flags;
        params.polyCount = m_pmesh.npolys;
        params.nvp = m_pmesh.nvp;
        params.detailMeshes = m_dmesh.meshes;
        params.detailVerts = m_dmesh.verts;
        params.detailVertsCount = m_dmesh.nverts;
        params.detailTris = m_dmesh.tris;
        params.detailTriCount = m_dmesh.ntris;
        params.walkableHeight = height; //Should add getter for this.
        params.walkableRadius = radius; //Should add getter for this.
        params.walkableClimb = maxClimb; //Should add getter for this.
        params.bmin = m_pmesh.bmin;
        params.bmax = m_pmesh.bmax;
        params.cs = cfg.cs; //Should add getter for this.
        params.ch = cfg.ch; //Should add getter for this.
        params.buildBvTree = true;
        
        MeshData meshData = NavMeshBuilder.createNavMeshData(params);
        navMesh = new NavMesh(meshData, params.nvp, 0);
        
        query = new NavMeshQuery(navMesh);
        
        try {
            MeshDataWriter mdw = new MeshDataWriter();
            mdw.write(new FileOutputStream(new File("test.md")),  meshData, ByteOrder.BIG_ENDIAN, false);
            MeshSetWriter msw = new MeshSetWriter();
            msw.write(new FileOutputStream(new File("test.nm")), navMesh, ByteOrder.BIG_ENDIAN, false);
        } catch (Exception ex) {
            LOG.error("[{}]", ex);
        }

        //Show wireframe. Helps with param tweaks. false = solid color.
        showDebugMeshes(meshData, true);
        
}
    
    private void buildSoloTest() {
        List<Integer> listTriLength = new ArrayList<>();
        List<AreaModification> areaMod = new ArrayList<>();
       
        SceneGraphVisitor visitor = new SceneGraphVisitor() {

            @Override
            public void visit(Spatial spat) {
                if (spat instanceof Geometry) {
                    //Load triangle lengths so we can pick them out from the 
                    //TriMesh later.
                    listTriLength.add(getTriangles(((Geometry) spat).getMesh()).length);
                    
                    /**
                     * Set Area Type based off materials in this case. UserData 
                     * can be added as a optional way to do this. UserData would 
                     * require separating the geometry in blender which is not 
                     * any different really than using materials. 
                     * 
                     * Doors could work the same way, mark the path between the 
                     * two rooms with a material or separate the door path 
                     * geometry into a separate object so it can be picked out. 
                     * 
                     * Off mesh connections can use a similar format. We could 
                     * parse the geometry looking for two connection geometry 
                     * that are flagged as same connection and set the off mesh 
                     * connections programmatically. 
                     */
                    String name = getModification(((Geometry) spat).getMaterial().getName());
                    System.out.println(name);
                    
                    switch (name) {
                        
                        case "water":
                            areaMod.add(SAMPLE_AREAMOD_WATER);
                            break;
                        case "road":
                            areaMod.add(SAMPLE_AREAMOD_ROAD);
                            break;
                        case "grass":
                            areaMod.add(SAMPLE_AREAMOD_GRASS);
                            break;
                        case "door":
                            areaMod.add(SAMPLE_AREAMOD_DOOR);
                            break;
                        case "jump":
                            areaMod.add(SAMPLE_AREAMOD_JUMP);
                            break;
                        default:
                            areaMod.add(SampleAreaModifications.SAMPLE_AREAMOD_GROUND);
                    }
                }
            }
        };
        
        ((SimpleApplication) getApplication()).getRootNode().getChild("worldmap").depthFirstTraversal(visitor);

        //Build merged mesh.
        InputGeomProvider geomProvider = new GeometryProviderBuilder(
                (Node)((SimpleApplication) getApplication()).getRootNode().getChild("worldmap")).build();
        
        RecastBuilderConfig bcfg = new RecastBuilderConfigBuilder(worldMap).withDetailMesh(true).
                build(new RecastConfigBuilder()
                        .withAgentRadius(.3f)           // r
                        .withAgentHeight(1.7f)          // h
                        //cs and ch should probably be .1 at min.
                        .withCellSize(.1f)              // cs=r/3
                        .withCellHeight(.1f)            // ch=cs 
                        .withAgentMaxClimb(.3f)         // > 2*ch
                        .withAgentMaxSlope(45f)         
                        .withEdgeMaxLen(2.4f)             // r*8
                        .withEdgeMaxError(1.3f)         // 1.1 - 1.5
                        .withDetailSampleDistance(8.0f) // increase if exception
                        .withDetailSampleMaxError(8.0f) // increase if exception
                        .withVertsPerPoly(3).build());
        
        //Split up for testing.
        RecastBuilderResult result = new RecastBuilder2().build(geomProvider, bcfg, listTriLength, areaMod);
        
        NavMeshDataCreateParamsBuilder navMeshDataCreateParamsBuilder = new NavMeshDataCreateParamsBuilder(result);
        PolyMesh m_pmesh = result.getMesh();
        
        //Set Ability flags.
        for (int i = 0; i < m_pmesh.npolys; ++i) {
            if (m_pmesh.areas[i] == SampleAreaModifications.SAMPLE_POLYAREA_TYPE_GROUND
              || m_pmesh.areas[i] == SampleAreaModifications.SAMPLE_POLYAREA_TYPE_GRASS
              || m_pmesh.areas[i] == SampleAreaModifications.SAMPLE_POLYAREA_TYPE_ROAD) {
                navMeshDataCreateParamsBuilder.withPolyFlag(i, SampleAreaModifications.SAMPLE_POLYFLAGS_WALK);
            } else if (m_pmesh.areas[i] == SampleAreaModifications.SAMPLE_POLYAREA_TYPE_WATER) {
                navMeshDataCreateParamsBuilder.withPolyFlag(i, SampleAreaModifications.SAMPLE_POLYFLAGS_SWIM);
            } else if (m_pmesh.areas[i] == SampleAreaModifications.SAMPLE_POLYAREA_TYPE_DOOR) {
                navMeshDataCreateParamsBuilder.withPolyFlags(i, SampleAreaModifications.SAMPLE_POLYFLAGS_WALK
                | SampleAreaModifications.SAMPLE_POLYFLAGS_DOOR);
            }
            if (m_pmesh.areas[i] > 0) {
                m_pmesh.areas[i]--;
            }
        }
        
        //Must either set variables for parameters walkableHeight, 
        //walkableRadius, walkableClimb manually for mesh data unless 
        //jme3-recast4j fixed.
        NavMeshDataCreateParams params = navMeshDataCreateParamsBuilder.build(bcfg);
//        params.walkableClimb = maxClimb; //Should add getter for this.
//        params.walkableHeight = height; //Should add getter for this.
//        params.walkableRadius = radius; //Should add getter for this.
        MeshData meshData = NavMeshBuilder.createNavMeshData(params);
        navMesh = new NavMesh(meshData, bcfg.cfg.maxVertsPerPoly, 0);
        query = new NavMeshQuery(navMesh);
        
        try {
            MeshDataWriter mdw = new MeshDataWriter();
            mdw.write(new FileOutputStream(new File("test.md")),  meshData, ByteOrder.BIG_ENDIAN, false);
            MeshSetWriter msw = new MeshSetWriter();
            msw.write(new FileOutputStream(new File("test.nm")), navMesh, ByteOrder.BIG_ENDIAN, false);
        } catch (Exception ex) {
            LOG.error("[{}]", ex);
        }

        //Show wireframe. Helps with param tweaks. false = solid color.
        showDebugMeshes(meshData, true);
    }

    /**
     * Get all triangles from a mesh. Should open up jme3-recast4j existing 
     * GeometryProviderBuilder method.
     *
     * @param mesh Mesh to get triangles from.
     * @return Returns array of triangles.
     */
    private int[] getTriangles(Mesh mesh) {
        int[] indices = new int[3];
        int[] triangles = new int[mesh.getTriangleCount() * 3];

        for (int i = 0; i < triangles.length; i += 3) {
            mesh.getTriangle(i / 3, indices);
            triangles[i] = indices[0];
            triangles[i + 1] = indices[1];
            triangles[i + 2] = indices[2];
        }
        return triangles;
    }
    
    private void buildTiled() {
        
        List<Integer> listTriLength = new ArrayList<>();
        List<AreaModification> areaMod = new ArrayList<>();
       
        SceneGraphVisitor visitor = new SceneGraphVisitor() {

            @Override
            public void visit(Spatial spat) {
                if (spat instanceof Geometry) {
                    //Load triangle lengths so we can pick them out from the 
                    //TriMesh later.
                    listTriLength.add(getTriangles(((Geometry) spat).getMesh()).length);
                    
                    /**
                     * Set Area Type based off materials in this case. UserData 
                     * can be added as a optional way to do this. UserData would 
                     * require separating the geometry in blender which is not 
                     * any different really than using materials. 
                     * 
                     * Doors could work the same way, mark the path between the 
                     * two rooms with a material or separate the door path 
                     * geometry into a separate object so it can be picked out. 
                     * 
                     * Off mesh connections can use a similar format. We could 
                     * parse the geometry looking for two connection geometry 
                     * that are flagged as same connection and set the off mesh 
                     * connections programmatically. 
                     */
                    String name = getModification(((Geometry) spat).getMaterial().getName());
                    System.out.println(name);
                    
                    switch (name) {
                        
                        case "water":
                            areaMod.add(SAMPLE_AREAMOD_WATER);
                            break;
                        case "road":
                            areaMod.add(SAMPLE_AREAMOD_ROAD);
                            break;
                        case "grass":
                            areaMod.add(SAMPLE_AREAMOD_GRASS);
                            break;
                        case "door":
                            areaMod.add(SAMPLE_AREAMOD_DOOR);
                            break;
                        case "jump":
                            areaMod.add(SAMPLE_AREAMOD_JUMP);
                            break;
                        default:
                            areaMod.add(SampleAreaModifications.SAMPLE_AREAMOD_GROUND);
                    }
                }
            }
        };
        
        ((SimpleApplication) getApplication()).getRootNode().getChild("worldmap").depthFirstTraversal(visitor);
        
        //Step 1. Gather our geometry.
        InputGeomProvider geomProvider = new GeometryProviderBuilder(worldMap).build();
        //Step 2. Create a Recast configuration object.
        RecastConfigBuilder builder = new RecastConfigBuilder();
        //Instantiate the configuration parameters.
        RecastConfig cfg = builder
                .withAgentRadius(.3f)       // r
                .withAgentHeight(1.7f)       // h
                //cs and ch should be .1 at min.
                .withCellSize(0.1f)                 // cs=r/2
                .withCellHeight(0.1f)               // ch=cs/2 but not < .1f
                .withAgentMaxClimb(.3f)   // > 2*ch
                .withAgentMaxSlope(45f)
                .withEdgeMaxLen(3.2f)               // r*8
                .withEdgeMaxError(1.3f)             // 1.1 - 1.5
                .withDetailSampleDistance(6.0f)     // increase if exception
                .withDetailSampleMaxError(6.0f)     // increase if exception
                .withVertsPerPoly(3)
                .withTileSize(16).build(); 
        // Build all tiles
        RecastBuilder rb = new RecastBuilder2(new ProgressListen());
        RecastBuilderResult[][] rcResult = rb.buildTiles(geomProvider, cfg, 1);
        // Add tiles to nav mesh
        int tw = rcResult.length;
        int th = rcResult[0].length;
        // Create empty nav mesh
        NavMeshParams navMeshParams = new NavMeshParams();
        copy(navMeshParams.orig, geomProvider.getMeshBoundsMin());
        navMeshParams.tileWidth = cfg.tileSize * cfg.cs;
        navMeshParams.tileHeight = cfg.tileSize * cfg.cs;
        navMeshParams.maxTiles = tw * th;
        navMeshParams.maxPolys = 32768;
        navMesh = new NavMesh(navMeshParams, 3);
        
        for (int y = 0; y < th; y++) {
            for (int x = 0; x < tw; x++) {
                PolyMesh pmesh = rcResult[x][y].getMesh();
                if (pmesh.npolys == 0) {
                        continue;
                }
                
                // Update poly flags from areas.
                for (int i = 0; i < pmesh.npolys; ++i) {
                    if (pmesh.areas[i] == SampleAreaModifications.SAMPLE_POLYAREA_TYPE_GROUND
                            || pmesh.areas[i] == SampleAreaModifications.SAMPLE_POLYAREA_TYPE_GRASS
                            || pmesh.areas[i] == SampleAreaModifications.SAMPLE_POLYAREA_TYPE_ROAD) {
                        pmesh.flags[i] = SampleAreaModifications.SAMPLE_POLYFLAGS_WALK;
                    } else if (pmesh.areas[i] == SampleAreaModifications.SAMPLE_POLYAREA_TYPE_WATER) {
                        pmesh.flags[i] = SampleAreaModifications.SAMPLE_POLYFLAGS_SWIM;
                    } else if (pmesh.areas[i] == SampleAreaModifications.SAMPLE_POLYAREA_TYPE_DOOR) {
                        pmesh.flags[i] = SampleAreaModifications.SAMPLE_POLYFLAGS_WALK
                                | SampleAreaModifications.SAMPLE_POLYFLAGS_DOOR;
                    }
                    if (pmesh.areas[i] > 0) {
                        pmesh.areas[i]--;
                    }
                }
                
                NavMeshDataCreateParams params = new NavMeshDataCreateParams();
                params.verts = pmesh.verts;
                params.vertCount = pmesh.nverts;
                params.polys = pmesh.polys;
                params.polyAreas = pmesh.areas;
                params.polyFlags = pmesh.flags;
                params.polyCount = pmesh.npolys;
                params.nvp = pmesh.nvp;
                PolyMeshDetail dmesh = rcResult[x][y].getMeshDetail();
                params.detailMeshes = dmesh.meshes;
                params.detailVerts = dmesh.verts;
                params.detailVertsCount = dmesh.nverts;
                params.detailTris = dmesh.tris;
                params.detailTriCount = dmesh.ntris;
                params.walkableHeight = cfg.walkableHeight;
                params.walkableRadius = cfg.walkableRadius;
                params.walkableClimb = cfg.walkableClimb;
                params.bmin = pmesh.bmin;
                params.bmax = pmesh.bmax;
                params.cs = cfg.cs;
                params.ch = cfg.ch;
                params.tileX = x;
                params.tileY = y;
                params.buildBvTree = true;
                navMesh.addTile(NavMeshBuilder.createNavMeshData(params), 0, 0);
            }
        }
        
        query = new NavMeshQuery(navMesh);

        try {
            //Native format using tiles.
            MeshSetWriter msw = new MeshSetWriter();
            msw.write(new FileOutputStream(new File("test.nm")), navMesh, ByteOrder.BIG_ENDIAN, false);
            //Read in saved NavMesh.
            MeshSetReader msr = new MeshSetReader();
            NavMesh navMeshFromSaved = msr.read(new FileInputStream("test.nm"), 3);
            int maxTiles = navMeshFromSaved.getMaxTiles();
            System.out.println("Tile count " + navMeshFromSaved.getTileCount());

            //Tile data can be null since maxTiles is not an exact science.
            for (int i = 0; i < maxTiles; i++) {
                MeshData meshdata = navMeshFromSaved.getTile(i).data;

                System.out.println("Tile " + i);
                if (meshdata != null ) {
                    showDebugMeshes(meshdata, true);
                }
            }
        }  catch (IOException ex) {
            LOG.info("{} {}", CrowdBuilderState.class.getName(), ex);
        }
    }
    
    private class ProgressListen implements RecastBuilderProgressListener {

        private long time = System.nanoTime();
        private long elapsedTime;
        private long avBuildTime;
        private long estTotalTime;
        private long estTimeRemain;
        private long buildTimeNano;
        private long elapsedTimeHr;
        private long elapsedTimeMin;
        private long elapsedTimeSec;
        private long totalTimeHr;
        private long totalTimeMin;
        private long totalTimeSec;
        private long timeRemainHr;
        private long timeRemainMin;
        private long timeRemainSec;

        @Override
        public void onProgress(int completed, int total) {
            elapsedTime += System.nanoTime() - time;
            avBuildTime = elapsedTime/(long)completed;
            estTotalTime = avBuildTime * (long)total;
            estTimeRemain = estTotalTime - elapsedTime;

            buildTimeNano = TimeUnit.MILLISECONDS.convert(avBuildTime, TimeUnit.NANOSECONDS);
            System.out.printf("Completed %d[%d] Average [%dms] ", completed, total, buildTimeNano);

            elapsedTimeHr = TimeUnit.HOURS.convert(elapsedTime, TimeUnit.NANOSECONDS) % 24;
            elapsedTimeMin = TimeUnit.MINUTES.convert(elapsedTime, TimeUnit.NANOSECONDS) % 60;
            elapsedTimeSec = TimeUnit.SECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS) % 60;
            System.out.printf("Elapsed Time [%02d:%02d:%02d] ", elapsedTimeHr, elapsedTimeMin, elapsedTimeSec);

            totalTimeHr = TimeUnit.HOURS.convert(estTotalTime, TimeUnit.NANOSECONDS) % 24;
            totalTimeMin = TimeUnit.MINUTES.convert(estTotalTime, TimeUnit.NANOSECONDS) % 60;
            totalTimeSec = TimeUnit.SECONDS.convert(estTotalTime, TimeUnit.NANOSECONDS) % 60;
            System.out.printf("Estimated Total [%02d:%02d:%02d] ", totalTimeHr, totalTimeMin, totalTimeSec);

            timeRemainHr = TimeUnit.HOURS.convert(estTimeRemain, TimeUnit.NANOSECONDS) % 24;
            timeRemainMin = TimeUnit.MINUTES.convert(estTimeRemain, TimeUnit.NANOSECONDS) % 60;
            timeRemainSec = TimeUnit.SECONDS.convert(estTimeRemain, TimeUnit.NANOSECONDS) % 60;
            System.out.printf("Remaining Time [%02d:%02d:%02d]%n", timeRemainHr, timeRemainMin, timeRemainSec);

            //reset time
            time = System.nanoTime();
        }
        
    }
        
    /**
     * Returns the Location on the Map which is currently under the Cursor. 
     * For this we use the Camera to project the point onto the near and far 
     * plane (because we don'from have the depth information [map height]). Then 
     * we can use this information to do a raycast, ideally the world is in 
     * between those planes and we hit it at the correct place.
     * 
     * @return The Location on the Map
     */
    public Vector3f getLocationOnMap() {
        Vector3f worldCoordsNear = getApplication().getCamera().getWorldCoordinates(getApplication().getInputManager().getCursorPosition(), 0);
        Vector3f worldCoordsFar = getApplication().getCamera().getWorldCoordinates(getApplication().getInputManager().getCursorPosition(), 1);

        // From closest at the camera to most far away
        Ray mouseRay = new Ray(worldCoordsNear, worldCoordsFar.subtractLocal(worldCoordsNear).normalizeLocal());
        CollisionResults cr = new CollisionResults();
        worldMap.collideWith(mouseRay, cr);

        if (cr.size() > 0) {
            return cr.getClosestCollision().getContactPoint();
        } else {
            return null;
        }
    }

    /**
     * Helper method to place a colored box at a specific location and fill the pathGeometries list with it,
     * so that later on we can remove all existing pathGeometries (from a previous path finding)
     *
     * @param color The color the box should have
     * @param position The position where the box will be placed
     * @return the box
     */
    public Geometry placeColoredBoxAt(ColorRGBA color, Vector3f position) {
        Geometry result = new Geometry("Box", new Box(0.25f, 0.25f, 0.25f));
        Material mat = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        result.setMaterial(mat);
        result.setLocalTranslation(position);
        pathGeometries.add(result);
        return result;
    }
    
    /**
     * Helper method to place a colored line between two specific locations and fill the pathGeometries list with it,
     * so that later on we can remove all existing pathGeometries (from a previous path finding)
     *
     * @param color The color the box should have
     * @param from The position where the line starts
     * @param to The position where the line is finished.
     * @return the line
     */
    public Geometry placeColoredLineBetween(ColorRGBA color, Vector3f from, Vector3f to) {
        Geometry result = new Geometry("Line", new Line(from, to));
        Material mat = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        mat.getAdditionalRenderState().setLineWidth(2f);
        result.setMaterial(mat);
        pathGeometries.add(result);
        return result;
    }
    
    @Override
    protected void onDisable() {
        //Called when the state was previously enabled but is now disabled 
        //either because setEnabled(false) was called or the state is being 
        //cleaned up.
    }
    
    @Override
    public void update(float tpf) {
        //TODO: implement behavior during runtime
    }

    /**
     * @return the characters
     */
    public List<Node> getCharacters() {
        return characters;
    }

}
