package com.jme3.recast4j.demo;

import com.jme3.app.DebugKeysAppState;
import com.jme3.app.LostFocusBehavior;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.audio.AudioListenerState;
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
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.recast4j.Detour.Crowd.CrowdManager;
import com.jme3.recast4j.Detour.Crowd.Impl.CrowdManagerAppstate;
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
import com.jme3.scene.plugins.gltf.ExtrasLoader;
import com.jme3.scene.plugins.gltf.GltfModelKey;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture2D;
import com.jme3.water.WaterFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DemoApplication extends SimpleApplication {
    private Node worldMap;
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
//        loadNavMeshLevel();
//        loadDoors();
        loadFish();
        loadPond();
        loadPondSurface();
//        getStateManager().getState(BulletAppState.class).setDebugEnabled(true);
    }

    private void setupWorld() {
        worldMap = new Node();
        worldMap.setName("worldmap");
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

    private void loadNavMeshLevel() {  
        Node level = (Node) getAssetManager().loadModel("Models/Level/recast_level.j3o"); 
        level.addControl(new RigidBodyControl(0));
        getStateManager().getState(BulletAppState.class).getPhysicsSpace().add(level);
        worldMap.attachChild(level);
    }

    private void loadDoors() {
        //gltf loader test
        GltfModelKey modelKey = new GltfModelKey("Textures/Level/recast_door.gltf");
        ExtrasLoader extras = new GltfUserDataLoader();
        modelKey.setExtrasLoader(extras);
        Node doors = (Node) getAssetManager().loadModel(modelKey);
        doors.setName("doors");
        doors.addControl(new RigidBodyControl(0));
        getStateManager().getState(BulletAppState.class).getPhysicsSpace().add(doors);
        getRootNode().attachChild(doors);
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
