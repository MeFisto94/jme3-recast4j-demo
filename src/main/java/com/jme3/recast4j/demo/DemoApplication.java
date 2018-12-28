package com.jme3.recast4j.demo;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.app.SimpleApplication;
import com.jme3.light.AmbientLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.recast4j.Recast.RecastConfigBuilder;
import com.jme3.recast4j.Recast.RecastTest;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import org.recast4j.detour.MeshData;
import org.recast4j.detour.NavMesh;

public class DemoApplication extends SimpleApplication {

    Geometry worldMap;
    AnimChannel walkChannel;
    NavMesh navMesh;
    FilterPostProcessor fpp;
    Node character;

    public static void main(String[] args) {
        DemoApplication app = new DemoApplication();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        character = (Node)assetManager.loadModel("Models/Jaime.j3o");
        character.setLocalTranslation(0f, 5f, 0f);
        character.setLocalScale(0.5f);


        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setColor("Diffuse", ColorRGBA.Red);
        mat.setColor("Ambient", ColorRGBA.White);
        mat.setBoolean("UseMaterialColors", true);
        worldMap = (Geometry)assetManager.loadModel("Models/dune.j3o");
        worldMap.setLocalScale(0.01f);
        worldMap.setMaterial(mat);
        // @TODO: Dune.j3o does not have normals and thus no neat lighting.
        //TangentBinormalGenerator.generate(worldMap.getMesh());


        rootNode.addLight(new AmbientLight(ColorRGBA.White));
        // Doesn't work:
        //rootNode.addLight(new DirectionalLight(new Vector3f(0f, -1f, 0f), ColorRGBA.White));

        walkChannel = character.getControl(AnimControl.class).createChannel();
        walk(true);

        rootNode.attachChild(character);
        rootNode.attachChild(worldMap);

        getCamera().setLocation(new Vector3f(0f, 10f, 0f));
        getCamera().lookAtDirection(new Vector3f(0f, -1f, 0f), Vector3f.UNIT_Z);

        fpp = new FilterPostProcessor(assetManager);
        viewPort.addProcessor(fpp);
        fpp.addFilter(new SSAOFilter(1f, 1f, 0.1f, 0.1f));

        MeshData meshData = RecastTest.buildBlockingRenderThread(worldMap.getMesh());
        navMesh = new NavMesh(meshData, 0, 0);
        try {
            RecastTest.saveToFile(navMesh);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        //TODO: add update code
    }

    @Override
    public void simpleRender(RenderManager rm) {
        //TODO: add render code
    }

    protected void walk(boolean walking) {
        if (walking) {
            walkChannel.setAnim("Walk");
        } else {
            walkChannel.setAnim("");
        }
    }
}
