package com.jme3.recast4j.demo;

import com.jme3.app.DebugKeysAppState;
import com.jme3.app.LostFocusBehavior;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.audio.AudioListenerState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
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
import com.jme3.recast4j.Detour.Crowd.Crowd;
import com.jme3.recast4j.Detour.Crowd.CrowdManager;
import com.jme3.recast4j.Detour.Crowd.Impl.CrowdManagerAppstate;
import com.jme3.recast4j.Detour.Crowd.MovementApplicationType;
import com.jme3.recast4j.Detour.DetourUtils;
import com.jme3.recast4j.Recast.*;
import com.jme3.recast4j.demo.controls.CrowdBCC;
import com.jme3.recast4j.demo.controls.PhysicsAgentControl;
import com.jme3.recast4j.demo.states.AgentGridState;
import com.jme3.recast4j.demo.states.AgentParamState;
import com.jme3.recast4j.demo.states.CrowdState;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Line;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.event.DefaultMouseListener;
import com.simsilica.lemur.event.MouseEventControl;
import org.recast4j.detour.*;
import org.recast4j.detour.crowd.CrowdAgent;
import org.recast4j.detour.crowd.CrowdAgentParams;
import org.recast4j.recast.RecastBuilder;
import org.recast4j.recast.RecastBuilderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DemoApplication extends SimpleApplication {
    Crowd crowd;
    Spatial worldMap;
    NavMesh navMesh;
    NavMeshQuery query;
    FilterPostProcessor fpp;
    List<Node> characters;
    List<Geometry> pathGeometries;
    Logger LOG = LoggerFactory.getLogger(DemoApplication.class.getName());
    CrowdManagerAppstate crowdManagerAppstate;
    Node player;
    
    public DemoApplication() {
        super( 
                new StatsAppState(),
                new AudioListenerState(),
                new DebugKeysAppState(),
                new LemurConfigState(),
                new GuiUtilState()
        );
        pathGeometries = new ArrayList<>(64);
        characters = new ArrayList<>(64);        
    }

    public static void main(String[] args) {
        DemoApplication app = new DemoApplication();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Recast Level Demo");
        settings.setGammaCorrection(true);
        //While testing, remove when ready for release.
        settings.setWidth(1280);
        settings.setHeight(720);
        app.setSettings(settings);
        app.start();
        app.setLostFocusBehavior(LostFocusBehavior.Disabled);
        app.setPauseOnLostFocus(false);
    }

    @Override
    public void simpleInitApp() {
        initKeys();
        crowdManagerAppstate = new CrowdManagerAppstate(new CrowdManager());
        getStateManager().attach(crowdManagerAppstate);
        //Set the atmosphere of the world, lights, camera, post processing, physics.
        setupWorld();

//        // This should be a click action in a GUI somewhere.
//        for (int i = 0; i < 5; i++) {
//            //Load various models from here.
//            addAJaime(i);
//        }
        loadJaime();
        //Load or build mesh objects used for navigation here.
        //Must be called prior to running recast navMesh build procedure.
        //Must set worldMap variable from method call.

        //loadNavMeshBox();
        //loadNavMeshDune();
        loadNavMeshLevel();
        loadDoors();

        getStateManager().getState(BulletAppState.class).setDebugEnabled(true);
        
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

        //Uncomment for 3rd person view. Call after player/navmesh is loaded.
        getStateManager().attach(new ThirdPersonCamState(player));
        
        try {
            RecastTest.saveToFile(navMesh);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        //Show wireframe. Helps with param tweaks.
        showDebugMeshes(meshData, true);
        System.out.println("Building succeeded after " + (System.currentTimeMillis() - time) + " ms");

        MouseEventControl.addListenersToSpatial(worldMap, new DefaultMouseListener() {
            @Override
            protected void click(MouseButtonEvent event, Spatial target, Spatial capture) {
                super.click(event, target, capture);
                
                // First clear existing pathGeometries from the old path finding:
                pathGeometries.forEach(Geometry::removeFromParent);
                // Clicked on the map, so build a path to:
                Vector3f locOnMap = getLocationOnMap(); // Don't calculate three times
                LOG.info("Will walk from {} to {}", characters.get(0).getWorldTranslation(), locOnMap);
                rootNode.attachChild(placeColoredBoxAt(ColorRGBA.Green, characters.get(0).getWorldTranslation().add(0f, 0.5f, 0f)));
                rootNode.attachChild(placeColoredBoxAt(ColorRGBA.Yellow, locOnMap.add(0f, 0.5f, 0f)));

                if (characters.size() == 1) {
                    QueryFilter filter = new BetterDefaultQueryFilter();
                    FindNearestPolyResult startPoly = query.findNearestPoly(characters.get(0).getWorldTranslation().toArray(null), new float[]{0.5f, 0.5f, 0.5f}, filter);
                    FindNearestPolyResult endPoly = query.findNearestPoly(DetourUtils.toFloatArray(locOnMap), new float[]{0.5f, 0.5f, 0.5f}, filter);
                    if (startPoly.getNearestRef() == 0 || endPoly.getNearestRef() == 0) {
                        LOG.info("Neither Start or End reference can be 0. startPoly [{}] endPoly [{}]", startPoly, endPoly);
                        pathGeometries.forEach(Geometry::removeFromParent);
                    } else {
                        if (event.getButtonIndex() == MouseInput.BUTTON_LEFT) {
                            findPathImmediately(characters.get(0), filter, startPoly, endPoly);
                        } else if (event.getButtonIndex() == MouseInput.BUTTON_RIGHT) {
                            findPathSlicedPartial(characters.get(0), filter, startPoly, endPoly);
                        }
                    }
//                else {
//                    if (crowd == null) {
//                        crowd = new Crowd(MovementApplicationType.BETTER_CHARACTER_CONTROL, 5, 0.4f, navMesh);
//                        CrowdAgentParams params = new CrowdAgentParams();
//                        params.height = 1.8f;
//                        params.radius = 0.3f;
//                        params.maxSpeed = 2f;
//                        params.maxAcceleration = 8f;
//                        params.collisionQueryRange = params.radius * 12f;
//                        params.pathOptimizationRange = params.radius * 30f;
//                        params.updateFlags = CrowdAgentParams.DT_CROWD_ANTICIPATE_TURNS | CrowdAgentParams.DT_CROWD_OBSTACLE_AVOIDANCE; //|
//                        //CrowdAgentParams.DT_CROWD_OPTIMIZE_TOPO | CrowdAgentParams.DT_CROWD_OPTIMIZE_VIS | CrowdAgentParams.DT_CROWD_SEPARATION;
//                        params.obstacleAvoidanceType = 0;
//
//                        for (int i = 0; i < 5; i++) {
//                            CrowdAgent ca = crowd.createAgent(characters.get(i).getWorldTranslation(), params);
//                            crowd.setSpatialForAgent(ca, characters.get(i));
//                        }
//
//                        crowdManagerAppstate.getCrowdManager().addCrowd(crowd);
//                    }
//
//                    FindNearestPolyResult endPoly = query.findNearestPoly(DetourUtils.toFloatArray(locOnMap), new float[]{0.5f, 0.5f, 0.5f}, new BetterDefaultQueryFilter());
//                    // @TODO: RequestMoveToTarget shall automatically query the nearest polygon or accept a FindNearestPolyResult
//                    System.out.println(crowd.requestMoveToTarget(locOnMap, endPoly.getNearestRef()));
//                }
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
                    rootNode.attachChild(placeColoredLineBetween(ColorRGBA.Orange, oldPos.add(0f, 0.5f, 0f), nu.add(0f, 0.5f, 0f)));
                    if (p.getRef() != 0) { // if ref is 0, it's the end.
                        rootNode.attachChild(placeColoredBoxAt(ColorRGBA.Blue, nu.add(0f, 0.5f, 0f)));
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
                    rootNode.attachChild(placeColoredLineBetween(ColorRGBA.Orange, oldPos.add(0f, 0.5f, 0f), nu.add(0f, 0.5f, 0f)));
                    if (p.getRef() != 0) { // if ref is 0, it's the end.
                        rootNode.attachChild(placeColoredBoxAt(ColorRGBA.Blue, nu.add(0f, 0.5f, 0f)));
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
                    rootNode.attachChild(placeColoredLineBetween(ColorRGBA.Orange, oldPos.add(0f, 0.5f, 0f), nu.add(0f, 0.5f, 0f)));
                    if (p.getRef() != 0) { // if ref is 0, it's the end.
                        rootNode.attachChild(placeColoredBoxAt(ColorRGBA.Blue, nu.add(0f, 0.5f, 0f)));
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

    private void setupWorld() {
        BulletAppState bullet = new BulletAppState();
        // Performance is better when threading in parallel
        bullet.setThreadingType(BulletAppState.ThreadingType.PARALLEL);
        stateManager.attach(bullet);
        
        /* A white, directional light source */
        DirectionalLight sun = new DirectionalLight();
        sun.setDirection((new Vector3f(0.5f, -0.5f, -0.5f)).normalizeLocal());
        sun.setColor(ColorRGBA.White);
        rootNode.addLight(sun); 
        
        /* A white, directional light source */
        DirectionalLight sun2 = new DirectionalLight();
        sun2.setDirection((new Vector3f(-0.5f, -0.5f, 0.5f)).normalizeLocal());
        sun2.setColor(ColorRGBA.White);
        rootNode.addLight(sun2); 
        
        /* A white ambient light source. */
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(ColorRGBA.White.mult(.8f));
        rootNode.addLight(ambient); 

        getCamera().setLocation(new Vector3f(0f, 40f, 0f));
        getCamera().lookAtDirection(new Vector3f(0f, -1f, 0f), Vector3f.UNIT_Z);

        fpp = new FilterPostProcessor(assetManager);
        viewPort.addProcessor(fpp);
        fpp.addFilter(new SSAOFilter(1f, 1f, 0.1f, 0.1f));
    }

    private void showDebugMeshes(MeshData meshData, boolean wireframe) {
        Material matRed = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matRed.setColor("Color", ColorRGBA.Red);
        
        if (wireframe) {
            matRed.getAdditionalRenderState().setWireframe(true);
        }

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

    private void addAJaime(int idx) {
        Node tmp = (Node)assetManager.loadModel("Models/Jaime/Jaime.j3o");
        tmp.setLocalTranslation(idx * 0.5f, 5f * 0f, (idx % 2 != 0 ? 1f : 0f));
        //tmp.addControl(new BetterCharacterControl(0.3f, 1.5f, 20f)); // values taken from recast defaults

        //tmp.addControl(new PhysicsAgentControl());
        //getStateManager().getState(BulletAppState.class).getPhysicsSpace().add(tmp);
        rootNode.attachChild(tmp);
        characters.add(tmp);
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
   
    private void loadJaime() {
        player = (Node) getAssetManager().loadModel("Models/Jaime/Jaime.j3o");
        player.addControl(new BetterCharacterControl(0.3f, 1.5f, 20f)); // values taken from recast defaults
//        player.addControl(new CrowdBCC(0.3f, 1.5f, 20f)); // values taken from recast defaults
        player.addControl(new PhysicsAgentControl());
        getStateManager().getState(BulletAppState.class).getPhysicsSpace().add(player);
        getRootNode().attachChild(player);
        characters.add(player);
    }
    
    private ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean keyPressed, float tpf) {
            //This is a chain method of attaching states. CrowdState needs 
            //both AgentGridState and AgentParamState to be enabled 
            //before it can create its GUI. All AppStates do their own cleanup.
            //Lemur cleanup for all states is done from CrowdState.
            if (name.equals("crowd builder") && !keyPressed) {
                //Each state handles its own removal and cleanup.
                //CrowdState(onDisable)=>AgentParamState(onDisable)=>AgentGridState(onDisable)
                if (getStateManager().getState(AgentGridState.class) != null) {
                    getStateManager().getState(CrowdState.class).setEnabled(false);
                //If AgentGridState is not attached, it starts the chain from its 
                //enabled method as shown here.
                //AgentGridState(onEnable)=>AgentParamState(onEnable)=>CrowdState(onEnable)    
                } else {
                    getStateManager().attach(new AgentGridState());
                }
            }
            
            if (name.equals("crowd pick") && !keyPressed) {
                if (getStateManager().getState(AgentParamState.class) != null) {
                    Vector3f locOnMap = getLocationOnMap(); // Don't calculate three times
                    getStateManager().getState(AgentParamState.class).setFieldTargetXYZ(locOnMap);
                }
            }
        }
    };

    private void initKeys() {
        getInputManager().addMapping("crowd builder", new KeyTrigger(KeyInput.KEY_F1));
        getInputManager().addMapping("crowd pick", new KeyTrigger(KeyInput.KEY_LSHIFT));
        getInputManager().addListener(actionListener, "crowd builder", "crowd pick");
    }
}
