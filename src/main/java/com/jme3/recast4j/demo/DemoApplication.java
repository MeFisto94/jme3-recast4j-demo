package com.jme3.recast4j.demo;

import com.jme3.animation.SkeletonControl;
import com.jme3.app.DebugKeysAppState;
import com.jme3.app.LostFocusBehavior;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.audio.AudioListenerState;
import com.jme3.bounding.BoundingBox;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.recast4j.Detour.Crowd.CrowdManager;
import com.jme3.recast4j.Detour.Crowd.Impl.CrowdManagerAppstate;
import com.jme3.recast4j.demo.controls.DoorSwingControl;
import com.jme3.recast4j.demo.controls.PhysicsAgentControl;
import com.jme3.recast4j.demo.states.AgentGridState;
import com.jme3.recast4j.demo.states.AgentParamState;
import com.jme3.recast4j.demo.states.CrowdBuilderState;
import com.jme3.recast4j.demo.states.tutorial.CrowdState;
import com.jme3.recast4j.demo.states.GuiUtilState;
import com.jme3.recast4j.demo.states.LemurConfigState;
import com.jme3.recast4j.demo.states.NavState;
import com.jme3.recast4j.demo.states.ThirdPersonCamState;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.SceneGraphVisitorAdapter;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture2D;
import com.jme3.water.WaterFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DemoApplication extends SimpleApplication {
    
    private final Quaternion YAW180 = new Quaternion().fromAngleAxis(FastMath.PI, new Vector3f(0,1,0));
    private Node worldMap, doorNode;
    Logger LOG = LoggerFactory.getLogger(DemoApplication.class.getName());
    
    public DemoApplication() {
        super( 
                new StatsAppState(),
                new AudioListenerState(),
                new DebugKeysAppState(),
                new NavState(),
                new CrowdManagerAppstate(new CrowdManager()),
                new LemurConfigState(),
                /*new CrowdState(),*/
                new GuiUtilState(),
                new ThirdPersonCamState()
        );
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
        //Set the atmosphere of the world, lights, camera, post processing, physics.
        setupWorld();
//        loadNavMeshBox();
//        loadNavMeshDune();
        loadJaime();
        loadNavMeshLevel();
//        loadPond();
//        loadPondSurface();
//        getStateManager().getState(BulletAppState.class).setDebugEnabled(true);
    }

    private void setupWorld() {
        worldMap = new Node("worldmap");
        getRootNode().attachChild(worldMap);
        
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
        
//        FilterPostProcessor fpp = new FilterPostProcessor(getAssetManager());
//        getViewPort().addProcessor(fpp);
//        fpp.addFilter(setupWater());
    }

    @Override
    public void simpleUpdate(float tpf) { }

    @Override
    public void simpleRender(RenderManager rm) {
    }
    
    private void initKeys() {
        getInputManager().addMapping("crowd builder", new KeyTrigger(KeyInput.KEY_F1));
        getInputManager().addMapping("crowd pick", new KeyTrigger(KeyInput.KEY_LSHIFT));
        getInputManager().addListener(actionListener, "crowd builder", "crowd pick");
    }

    private void addAJaime(int idx) {
        Node tmp = (Node)assetManager.loadModel("Models/Jaime/Jaime.j3o");
        tmp.setLocalTranslation(idx * 0.5f, 5f * 0f, (idx % 2 != 0 ? 1f : 0f));
        //tmp.addControl(new BetterCharacterControl(0.3f, 1.5f, 20f)); // values taken from recast defaults

        //tmp.addControl(new PhysicsAgentControl());
        //getStateManager().getState(BulletAppState.class).getPhysicsSpace().add(tmp);
        rootNode.attachChild(tmp);
        getStateManager().getState(NavState.class).getCharacters().add(tmp);
    }

    private void loadNavMeshBox() {
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setColor("Diffuse", ColorRGBA.Red);
        mat.setColor("Ambient", ColorRGBA.White);
        mat.setBoolean("UseMaterialColors", true);
        
        Geometry worldMapGeo = new Geometry("", new Box(8f, 1f, 8f));
        worldMapGeo.setMaterial(mat);
        worldMapGeo.addControl(new RigidBodyControl(0f));
        
        getStateManager().getState(BulletAppState.class).getPhysicsSpace().add(worldMapGeo);
        
        worldMap.attachChild(worldMapGeo);
    }

//    private void loadNavMeshDune() {
//        worldMap = (Geometry)assetManager.loadModel("Models/dune.j3o");
//        // @TODO: Dune.j3o does not have normals and thus no neat lighting.
//        TangentBinormalGenerator.generate(worldMap.getMesh());
//    }

    /**
     * Loads the room scene and adds, rotates, and moves the doors into place.
     */
    private void loadNavMeshLevel() {  
        Node level = (Node) getAssetManager().loadModel("Models/Level/recast_level.mesh.j3o"); 
        level.addControl(new RigidBodyControl(0));
        getStateManager().getState(BulletAppState.class).getPhysicsSpace().add(level);
        worldMap.attachChild(level);
        
        /**
         * Create door node here since NavState checks the door node for null 
         * to avoid trying to add MouseEventControl when no door node is used. 
         * Like when loading different scenes such as the pond. The nodes name 
         * is used for locating it as a child of rootNode.
         */
        doorNode = new Node("doorNode");
        getRootNode().attachChild(doorNode);

        /**
         * Creating doors in blender with their origin at (0,0,0) is required.
         * The findPolysAroundCircle method in MouseEventControl uses the doors 
         * origin to localize the search for door polys so doors must be moved 
         * into their final position. If you apply location in blender, then 
         * findPolysAroundCircle would start the search from (0,0,0).
         */
        loadDoor(new Vector3f(-.39f, 0f, -10.03f), null);
        loadDoor(new Vector3f(-15.49f, 2.7f, -2.23f), null);
        loadDoor(new Vector3f(-21.49f, 2.7f, -2.23f), null);
        loadDoor(new Vector3f(-22.51f, 2.7f, 2.23f), YAW180);
        loadDoor(new Vector3f(-16.51f, 2.7f, 2.23f), YAW180);
    }

    /**
     * Adds a door to the scene at the given location and rotation.
     * 
     * @param location Where to move the door to.
     * @param rotation The doors rotation. A null value will keep the doors 
     * current rotation.
     */
    private void loadDoor(Vector3f location, Quaternion rotation) {
        
        /**
         * gltf loader test. This works but gltf doesn't when it comes to 
         * exporting animations created in blender. Imported animations into 
         * blender that are then exported do work however.
         */
//        GltfModelKey modelKey = new GltfModelKey("Textures/Level/Door.gltf");
//        ExtrasLoader extras = new GltfUserDataLoader();
//        modelKey.setExtrasLoader(extras);
        
        //Load a door.
        Node door = (Node) getAssetManager().loadModel("Models/Level/Door.mesh.j3o");
        door.setName("door");
        
        /**
         * Couldn't get hardware skinning to turn off which would allow the 
         * bounding box to move with the door as it opens or closes so added
         * a hitBox to the root bone instead.
         */
        door.depthFirstTraversal(new SceneGraphVisitorAdapter() {
            @Override
            public void visit(Node node) {

                if (node.getControl(SkeletonControl.class) != null) {
                    
                    SkeletonControl skelControl = node.getControl(SkeletonControl.class);
                    
                    //Create a box shape with the same dimensions as the door.
                    BoundingBox bounds = (BoundingBox) door.getWorldBound();
                    Box boxMesh = new Box(bounds.getXExtent(),bounds.getYExtent(),bounds.getZExtent()); 
                    
                    //The geometry for the door.
                    Geometry boxGeo = new Geometry("hitBox", boxMesh); 
                    
                    //The material.
                    Material boxMat = new Material(getAssetManager(), "Common/MatDefs/Light/Lighting.j3md"); 
                    boxMat.setBoolean("UseMaterialColors", true); 
                    boxMat.setColor("Ambient", ColorRGBA.Green); 
                    boxMat.setColor("Diffuse", ColorRGBA.Green); 
                    boxGeo.setMaterial(boxMat); 
                    //Toggle visibility.
                    boxGeo.setCullHint(Spatial.CullHint.Always);
                    
                    //Center hitBox to door.
                    boxGeo.setLocalTranslation(bounds.getCenter());
                    
                    /**
                     * Create a node that will use the same origin as the root
                     * bone which has the same origin as the door. This will 
                     * keep the searches in MouseEventControl localized to this 
                     * door.
                     */
                    Node collisionNode = new Node("collisionNode");
                    collisionNode.attachChild(boxGeo);
                    
                    //Our root bone for the animations.
                    skelControl.getAttachmentsNode("Root").attachChild(collisionNode);
                }
            }
        });
        
        /**
         * Creating doors in blender with their origin at (0,0,0) is required.
         * The findPolysAroundCircle method in MouseEventControl uses the doors 
         * origin to localize the search for door polys so doors must be moved 
         * into their final position. If you apply location in blender, then 
         * findPolysAroundCircle would start the search from (0,0,0).
         */ 
        door.setLocalTranslation(location);
        
        //Some doors need rotating.
        if (rotation != null) {
            door.setLocalRotation(rotation);
        }
        
        //Add our animation swing control and attach to rootNode.
        door.addControl(new DoorSwingControl());
        doorNode.attachChild(door);
    }      
    
    private <T extends Control> T findControl(Spatial s, Class<T> controlClass) {
        T ctrl = s.getControl(controlClass);
        if (ctrl != null) {
            return ctrl;
        }
        if (s instanceof Node) {
            Node n = (Node) s;
            for (Spatial spatial : n.getChildren()) {
                ctrl = findControl(spatial, controlClass);
                if (ctrl != null) {
                    return ctrl;
                }
            }
        }
        return null;
    }
    
    private void loadJaime() {
        Node player = (Node) getAssetManager().loadModel("Models/Jaime/Jaime.j3o");
        player.setName("player");
        player.addControl(new BetterCharacterControl(0.3f, 1.5f, 20f)); // values taken from recast defaults
//        player.addControl(new CrowdBCC(0.3f, 1.5f, 20f)); // values taken from recast defaults
        player.addControl(new PhysicsAgentControl());
        getStateManager().getState(BulletAppState.class).getPhysicsSpace().add(player);
        getStateManager().getState(NavState.class).getCharacters().add(player);
        getRootNode().attachChild(player);
    }
    
    private void loadFish() {
        Node fish = (Node) getAssetManager().loadModel("Models/Fish/Fish1.j3o");
        fish.setName("fish");
        fish.setLocalTranslation(-8f, -.2f, 0f);
//        fish.addControl(new BetterCharacterControl(.2f, .4f, 1f));
//        fish.addControl(new PhysicsAgentControl());
//        getStateManager().getState(BulletAppState.class).getPhysicsSpace().add(fish);
        getRootNode().attachChild(fish);
    }
    
    private void loadPond() {
        Node pond = (Node) getAssetManager().loadModel("Models/Pond/pond_ground.j3o"); 
        pond.addControl(new RigidBodyControl(0));
        getStateManager().getState(BulletAppState.class).getPhysicsSpace().add(pond);
        worldMap.attachChild(pond);
    }

    private void loadPondSurface() {
        Node surface = (Node) getAssetManager().loadModel("Models/Pond/pond_surface.j3o");
        surface.setName("water");
        Vector3f localTranslation = surface.getLocalTranslation();
        surface.setLocalTranslation(localTranslation.x, 4f, localTranslation.z);
        surface.addControl(new RigidBodyControl(0));
        getStateManager().getState(BulletAppState.class).getPhysicsSpace().add(surface);
        worldMap.attachChild(surface);
    }
    
    private ActionListener actionListener = new ActionListener() {
        @Override
        public void onAction(String name, boolean keyPressed, float tpf) {
            //This is a chain method of attaching states. CrowdBuilderState needs 
            //both AgentGridState and AgentParamState to be enabled 
            //before it can create its GUI. All AppStates do their own cleanup.
            //Lemur cleanup for all states is done from CrowdBuilderState.
            //If we activate from key, the current build of navmesh will be used.
            if (name.equals("crowd builder") && !keyPressed) {
                //Each state handles its own removal and cleanup.
                //Check for AgentGridState.class first becasue if its enabled
                // all are enabled.
                //CrowdBuilderState(onDisable)=>AgentParamState(onDisable)=>AgentGridState(onDisable)
                if (getStateManager().getState(AgentGridState.class) != null) {
                    getStateManager().getState(CrowdBuilderState.class).setEnabled(false);
                //If AgentGridState is not attached, it starts the chain from its 
                //enabled method as shown here.
                //AgentGridState(onEnable)=>AgentParamState(onEnable)=>CrowdBuilderState(onEnable)    
                } else {
                    getStateManager().attach(new AgentGridState());
                }
            }
            
            if (name.equals("crowd pick") && !keyPressed) {
                if (getStateManager().getState(AgentParamState.class) != null) {
                    Vector3f locOnMap = getStateManager().getState(NavState.class).getLocationOnMap(); // Don't calculate three times
                    if (locOnMap != null) {
                        getStateManager().getState(AgentParamState.class).setFieldTargetXYZ(locOnMap);
                    }
                } 
                
                if (getStateManager().getState(CrowdState.class) != null) {
                    Vector3f locOnMap = getStateManager().getState(NavState.class).getLocationOnMap(); // Don't calculate three times
                    getStateManager().getState(CrowdState.class).setTarget(locOnMap);
                }
            }
        }
    };

    private WaterFilter setupWater() {
        //Water Filter
        WaterFilter waterPond = new WaterFilter(getRootNode(), new Vector3f(0.5f, -0.5f, -0.5f));

        //foam
        waterPond.setUseFoam(false);
        waterPond.setFoamTexture((Texture2D) getAssetManager().loadTexture("Common/MatDefs/Water/Textures/foam2.jpg"));
        waterPond.setFoamIntensity(0.4f);
        waterPond.setFoamHardness(0.3f);
        waterPond.setFoamExistence(new Vector3f(0.8f, 8f, 1f));
        //light reflection
        waterPond.setReflectionDisplace(50);
        waterPond.setRefractionConstant(0.25f);
        waterPond.setRefractionStrength(0.2f);
        //water color
        waterPond.setColorExtinction(new Vector3f(30, 50, 70));
        waterPond.setWaterColor(new ColorRGBA().setAsSrgb(0.0078f, 0.3176f, 0.5f, 1.0f));
        waterPond.setDeepWaterColor(new ColorRGBA().setAsSrgb(0.0039f, 0.00196f, 0.145f, 1.0f));
        waterPond.setWaterTransparency(0.12f);
        //underwater
        waterPond.setCausticsIntensity(0.4f);
        waterPond.setUnderWaterFogDistance(80);
        //waves
        waterPond.setUseRipples(true);
        waterPond.setSpeed(0.75f);
        waterPond.setWaterHeight(-.1f);
        waterPond.setMaxAmplitude(0.3f);
        waterPond.setWaveScale(0.008f);
        //translation and shorline
        waterPond.setCenter(new Vector3f(-7.6f, -1f, 0));
        waterPond.setRadius(6.75f);
        waterPond.setShapeType(WaterFilter.AreaShape.Circular);
        
        return waterPond;
    }

}
