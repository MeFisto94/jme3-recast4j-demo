package com.jme3.recast4j.demo;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResults;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.recast4j.Detour.BetterDefaultQueryFilter;
import com.jme3.recast4j.Detour.DetourUtils;
import com.jme3.recast4j.Recast.*;
import com.jme3.recast4j.demo.controls.NavMeshChaserControl;
import com.jme3.recast4j.demo.states.RecastGUIState;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Line;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.event.DefaultMouseListener;
import com.simsilica.lemur.event.MouseEventControl;
import org.recast4j.detour.*;
import org.recast4j.recast.RecastBuilder;
import org.recast4j.recast.RecastBuilderConfig;

import java.util.ArrayList;
import java.util.List;

public class DemoApplication extends SimpleApplication {

//    Geometry worldMap;
    Spatial worldMap;
    NavMesh navMesh;
    NavMeshQuery query;
    FilterPostProcessor fpp;
    Node character;
    List<Geometry> pathGeometries;

    public DemoApplication() {
        pathGeometries = new ArrayList<>(64);
    }

    public static void main(String[] args) {
        DemoApplication app = new DemoApplication();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("RecastLevel");
        settings.setGammaCorrection(true);
        app.setSettings(settings);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        GuiGlobals.initialize(this);
        flyCam.setEnabled(false);
        getStateManager().attach(new RecastGUIState());

        //Set the atmosphere of the world, lights, camera, post processing, physics.
        setupWorld();
        
        //Load various models from here.
        loadJaime();
        
        //Load or build mesh objects used for navigation here.
        //Must be called prior to running recast navMesh build procedure.
        //Must set worldMap variable from method call.
        //        loadNavMeshBox();
        //        loadNavMeshDune();
        loadNavMeshLevel();
        loadDoors();

        //getStateManager().getState(BulletAppState.class).setDebugEnabled(true);
        
        System.out.println("Building Nav Mesh, this may freeze your computer for a few seconds, please stand by");
        long time = System.currentTimeMillis(); // Never do real benchmarking with currentTimeMillis!
        RecastBuilderConfig bcfg = new RecastBuilderConfigBuilder((Node)worldMap).build(new RecastConfigBuilder().withVertsPerPoly(3).build());
        MeshData meshData = NavMeshBuilder.createNavMeshData(new NavMeshDataCreateParamsBuilder(new RecastBuilder().build(new GeometryProviderBuilder((Node)worldMap).build(), bcfg)).build(bcfg));
        navMesh = new NavMesh(meshData, bcfg.cfg.maxVertsPerPoly, 0);
        query = new NavMeshQuery(navMesh);

        try {
            RecastTest.saveToFile(navMesh);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        showDebugMeshes(meshData);
        System.out.println("Building succeeded after " + (System.currentTimeMillis() - time) + " ms");

        MouseEventControl.addListenersToSpatial(worldMap, new DefaultMouseListener() {
            @Override
            protected void click(MouseButtonEvent event, Spatial target, Spatial capture) {
                super.click(event, target, capture);
                // First clear existing pathGeometries from the old path finding:
                pathGeometries.forEach(Geometry::removeFromParent);

                // Clicked on the map, so build a path to:
                rootNode.attachChild(placeColoredBoxAt(ColorRGBA.Green, character.getWorldTranslation().add(0f, 0.5f, 0f)));
                rootNode.attachChild(placeColoredBoxAt(ColorRGBA.Yellow, getLocationOnMap().add(0f, 0.5f, 0f)));

                QueryFilter filter = new BetterDefaultQueryFilter();
                FindNearestPolyResult startPoly = query.findNearestPoly(character.getWorldTranslation().toArray(null), new float[] {0.5f, 0.5f, 0.5f}, filter);
                FindNearestPolyResult endPoly = query.findNearestPoly(getLocationOnMap().toArray(null), new float[] {0.5f, 0.5f, 0.5f}, filter);

                findPathImmediately(filter, startPoly, endPoly);
            }
        });
    }

    private void findPathImmediately(QueryFilter filter, FindNearestPolyResult startPoly, FindNearestPolyResult endPoly) {
        FindPathResult fpr = query.findPath(startPoly.getNearestRef(), endPoly.getNearestRef(), startPoly.getNearestPos(), endPoly.getNearestPos(), filter);
        if (fpr.getStatus().isSuccess()) {
            // Get the proper path from the rough polygon listing
            List<StraightPathItem> list = query.findStraightPath(startPoly.getNearestPos(), endPoly.getNearestPos(), fpr.getRefs(), Integer.MAX_VALUE, 0);
            Vector3f oldPos = character.getWorldTranslation();
            List<Vector3f> vector3fList = new ArrayList<>(list.size());

            if (!list.isEmpty()) {
                for (StraightPathItem p: list) {
                    Vector3f nu = DetourUtils.createVector3f(p.getPos());
                    rootNode.attachChild(placeColoredLineBetween(ColorRGBA.Orange, oldPos.add(0f, 0.5f, 0f), nu.add(0f, 0.5f, 0f)));
                    if (p.getRef() != 0) { // if ref is 0, it's the end.
                        rootNode.attachChild(placeColoredBoxAt(ColorRGBA.Blue, nu.add(0f, 0.5f, 0f)));
                    }
                    vector3fList.add(nu);
                    oldPos = nu;
                }

                character.getControl(NavMeshChaserControl.class).stopFollowing();
                character.getControl(NavMeshChaserControl.class).followPath(vector3fList);
            } else {
                System.err.println("Unable to find straight paths");
            }
        } else {
            System.err.println("I'm sorry, unable to find a path.....");
        }
    }

    private void findPathSliced(QueryFilter filter, FindNearestPolyResult startPoly, FindNearestPolyResult endPoly) {
        query.initSlicedFindPath(startPoly.getNearestRef(), endPoly.getNearestRef(), startPoly.getNearestPos(), endPoly.getNearestPos(), filter, 0);
        UpdateSlicedPathResult res;
        do {
            // typically called from a control or appstate, so simulate it with a loop and sleep.
            res = query.updateSlicedFindPath(1);
            try {
                Thread.sleep(10);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        } while (res.getStatus() == Status.IN_PROGRESS);

        FindPathResult fpr = query.finalizeSlicedFindPath();

        // @TODO: Use NavMeshSliceControl (but then how to do the Debug Graphics?)
        // @TODO: Try Partial. How would one make this logic with controls etc so it's easy?
        //query.finalizeSlicedFindPathPartial();

        if (fpr.getStatus().isSuccess()) {
            // Get the proper path from the rough polygon listing
            List<StraightPathItem> list = query.findStraightPath(startPoly.getNearestPos(), endPoly.getNearestPos(), fpr.getRefs(), Integer.MAX_VALUE, 0);
            Vector3f oldPos = character.getWorldTranslation();
            List<Vector3f> vector3fList = new ArrayList<>(list.size());

            if (!list.isEmpty()) {
                for (StraightPathItem p: list) {
                    Vector3f nu = DetourUtils.createVector3f(p.getPos());
                    rootNode.attachChild(placeColoredLineBetween(ColorRGBA.Orange, oldPos.add(0f, 0.5f, 0f), nu.add(0f, 0.5f, 0f)));
                    if (p.getRef() != 0) { // if ref is 0, it's the end.
                        rootNode.attachChild(placeColoredBoxAt(ColorRGBA.Blue, nu.add(0f, 0.5f, 0f)));
                    }
                    vector3fList.add(nu);
                    oldPos = nu;
                }

                character.getControl(NavMeshChaserControl.class).stopFollowing();
                character.getControl(NavMeshChaserControl.class).followPath(vector3fList);
            } else {
                System.err.println("Unable to find straight paths");
            }
        } else {
            System.err.println("I'm sorry, unable to find a path.....");
        }
    }

    private void setupWorld() {
        stateManager.attach(new BulletAppState());

        /** A white, directional light source */ 
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection((new Vector3f(0.5f, -0.5f, -0.5f)).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun); 
        
        /** A white, directional light source */ 
        DirectionalLight sun2 = new DirectionalLight();
        sun2.setDirection((new Vector3f(-0.5f, -0.5f, 0.5f)).normalizeLocal());
        sun2.setColor(ColorRGBA.White);
        rootNode.addLight(sun2); 
        
        /** A white ambient light source. */ 
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(.8f));
        rootNode.addLight(ambient); 

        getCamera().setLocation(new Vector3f(0f, 40f, 0f));
        getCamera().lookAtDirection(new Vector3f(0f, -1f, 0f), Vector3f.UNIT_Z);

        fpp = new FilterPostProcessor(assetManager);
        viewPort.addProcessor(fpp);
        fpp.addFilter(new SSAOFilter(1f, 1f, 0.1f, 0.1f));
    }

    private void showDebugMeshes(MeshData meshData) {
        Material matRed = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matRed.setColor("Color", ColorRGBA.Red);
        Material matGreen = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
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

        rootNode.attachChild(g);
        rootNode.attachChild(gDetailed);
    }

    @Override
    public void simpleUpdate(float tpf) { }

    @Override
    public void simpleRender(RenderManager rm) {
    }

    /**
     * Returns the Location on the Map which is currently under the Cursor. For this we use the Camera to project the
     * point onto the near and far plane (because we don't have the depth information [map height]). Then we can use
     * this information to do a raycast, ideally the world is in between those planes and we hit it at the correct place.
     * @return The Location on the Map
     */
    public Vector3f getLocationOnMap() {
        Vector3f worldCoordsNear = getCamera().getWorldCoordinates(inputManager.getCursorPosition(), 0);
        Vector3f worldCoordsFar = getCamera().getWorldCoordinates(inputManager.getCursorPosition(), 1);

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
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
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
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        mat.getAdditionalRenderState().setLineWidth(2f);
        result.setMaterial(mat);
        pathGeometries.add(result);
        return result;
    }

    private void loadJaime() {
        character = (Node)assetManager.loadModel("Models/Jaime.j3o");
        character.setLocalTranslation(0f, 5f, 0f);
        character.addControl(new BetterCharacterControl(0.3f, 2f, 20f)); // values taken from recast defaults
        character.addControl(new NavMeshChaserControl());
        getStateManager().getState(BulletAppState.class).getPhysicsSpace().add(character);
        rootNode.attachChild(character);
    }

    private void loadNavMeshBox() {
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setColor("Diffuse", ColorRGBA.Red);
        mat.setColor("Ambient", ColorRGBA.White);
        mat.setBoolean("UseMaterialColors", true);
        
        
        worldMap = new Geometry("", new Box(8f, 1f, 8f));
        worldMap.setMaterial(mat);
        worldMap.addControl(new RigidBodyControl(0f));
        
        getStateManager().getState(BulletAppState.class).getPhysicsSpace().add(worldMap);
        
        rootNode.attachChild(worldMap);
    }

    private void loadNavMeshDune() {
//        worldMap = (Geometry)assetManager.loadModel("Models/dune.j3o");
//        // @TODO: Dune.j3o does not have normals and thus no neat lighting.
//        TangentBinormalGenerator.generate(worldMap.getMesh());
    }

    private void loadNavMeshLevel() {  
        worldMap = getAssetManager().loadModel("Models/Level/recast_level.j3o");    
        worldMap.addControl(new RigidBodyControl(0));
        getStateManager().getState(BulletAppState.class).getPhysicsSpace().add(worldMap);
        getRootNode().attachChild(worldMap);
    }

    private void loadDoors() {
        Spatial doors = getAssetManager().loadModel("Models/Level/recast_door.j3o");
        doors.addControl(new RigidBodyControl(0));
        getStateManager().getState(BulletAppState.class).getPhysicsSpace().add(doors);
        getRootNode().attachChild(doors);
    }
    
}
