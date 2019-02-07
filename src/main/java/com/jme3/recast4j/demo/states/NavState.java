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
import com.jme3.recast4j.Recast.RecastTest;
import com.jme3.recast4j.Recast.RecastUtils;
import com.jme3.recast4j.demo.controls.PhysicsAgentControl;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Line;
import com.simsilica.lemur.event.DefaultMouseListener;
import com.simsilica.lemur.event.MouseEventControl;
import java.util.ArrayList;
import java.util.List;
import org.recast4j.detour.FindNearestPolyResult;
import org.recast4j.detour.MeshData;
import org.recast4j.detour.NavMesh;
import org.recast4j.detour.NavMeshBuilder;
import org.recast4j.detour.NavMeshDataCreateParams;
import org.recast4j.detour.NavMeshQuery;
import org.recast4j.detour.QueryFilter;
import org.recast4j.detour.Result;
import org.recast4j.detour.Status;
import org.recast4j.detour.StraightPathItem;
import org.recast4j.recast.RecastBuilder;
import org.recast4j.recast.RecastBuilderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Robert
 */
public class NavState extends BaseAppState {

    private static final Logger LOG = LoggerFactory.getLogger(NavState.class.getName());
    
    private Spatial worldMap;
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
        worldMap = ((SimpleApplication) getApplication()).getRootNode().getChild("worldmap");
        System.out.println("Building Nav Mesh, this may freeze your computer for a few seconds, please stand by");
        long time = System.currentTimeMillis(); // Never do real benchmarking with currentTimeMillis!
        RecastBuilderConfig bcfg = new RecastBuilderConfigBuilder((Node)worldMap).
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
                        .withDetailSampleDistance(6.0f) // increase if exception
                        .withDetailSampleMaxError(5.0f) // increase if exception
                        .withVertsPerPoly(3).build());
        
        //Split up for testing.
        NavMeshDataCreateParams build = new NavMeshDataCreateParamsBuilder(
                new RecastBuilder().build(new GeometryProviderBuilder((Node)worldMap).build(), bcfg)).build(bcfg);
        MeshData meshData = NavMeshBuilder.createNavMeshData(build);
        navMesh = new NavMesh(meshData, bcfg.cfg.maxVertsPerPoly, 0);
        query = new NavMeshQuery(navMesh);
        
        try {
            RecastTest.saveToFile(navMesh);
        } catch (Exception ex) {
            LOG.error("[{}]", ex);
        }

        //Show wireframe. Helps with param tweaks. false = solid color.
        showDebugMeshes(meshData, true);
        
        System.out.println("Building succeeded after " + (System.currentTimeMillis() - time) + " ms");
        
        System.out.println(worldMap);
        
        MouseEventControl.addListenersToSpatial(worldMap, new DefaultMouseListener() {
            @Override
            protected void click(MouseButtonEvent event, Spatial target, Spatial capture) {
                super.click(event, target, capture);
                
                // First clear existing pathGeometries from the old path finding:
                pathGeometries.forEach(Geometry::removeFromParent);
                // Clicked on the map, so build a path to:
                Vector3f locOnMap = getLocationOnMap(); // Don't calculate three times
                LOG.info("Will walk from {} to {}", getCharacters().get(0).getWorldTranslation(), locOnMap);
                ((SimpleApplication) getApplication()).getRootNode().attachChild(placeColoredBoxAt(ColorRGBA.Green, getCharacters().get(0).getWorldTranslation().add(0f, 0.5f, 0f)));
                ((SimpleApplication) getApplication()).getRootNode().attachChild(placeColoredBoxAt(ColorRGBA.Yellow, locOnMap.add(0f, 0.5f, 0f)));
                
                if (getCharacters().size() == 1) {
                    QueryFilter filter = new BetterDefaultQueryFilter();
                    FindNearestPolyResult startPoly = query.findNearestPoly(getCharacters().get(0).getWorldTranslation().toArray(null), new float[]{0.5f, 0.5f, 0.5f}, filter);
                    FindNearestPolyResult endPoly = query.findNearestPoly(DetourUtils.toFloatArray(locOnMap), new float[]{0.5f, 0.5f, 0.5f}, filter);
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
        
    /**
     * Returns the Location on the Map which is currently under the Cursor. For this we use the Camera to project the
     * point onto the near and far plane (because we don't have the depth information [map height]). Then we can use
     * this information to do a raycast, ideally the world is in between those planes and we hit it at the correct place.
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
