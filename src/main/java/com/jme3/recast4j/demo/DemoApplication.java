package com.jme3.recast4j.demo;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResults;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.light.AmbientLight;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.recast4j.Recast.*;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.event.DefaultMouseListener;
import com.simsilica.lemur.event.MouseEventControl;
import org.recast4j.detour.MeshData;
import org.recast4j.detour.NavMesh;
import org.recast4j.detour.NavMeshBuilder;
import org.recast4j.recast.RecastBuilder;
import org.recast4j.recast.RecastBuilderConfig;

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
        GuiGlobals.initialize(this);

        character = (Node)assetManager.loadModel("Models/Jaime.j3o");
        character.setLocalTranslation(0f, 5f, 0f);

        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setColor("Diffuse", ColorRGBA.Red);
        mat.setColor("Ambient", ColorRGBA.White);
        mat.setBoolean("UseMaterialColors", true);
        //worldMap = (Geometry)assetManager.loadModel("Models/dune.j3o");
        worldMap = new Geometry("", new Box(8f, 1f, 8f));
        //worldMap.setLocalScale(0.01f);
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

        getCamera().setLocation(new Vector3f(0f, 20f, 0f));
        getCamera().lookAtDirection(new Vector3f(0f, -1f, 0f), Vector3f.UNIT_Z);

        fpp = new FilterPostProcessor(assetManager);
        viewPort.addProcessor(fpp);
        fpp.addFilter(new SSAOFilter(1f, 1f, 0.1f, 0.1f));

        System.out.println("Building Nav Mesh, this may freeze your computer for a few seconds, please stand by");
        long time = System.currentTimeMillis(); // Never do real benchmarking with currentTimeMillis!
        //MeshData meshData = RecastTest.buildBlockingRenderThread(worldMap.getMesh());
        RecastBuilderConfig bcfg = new RecastBuilderConfigBuilder(worldMap).build(new RecastConfigBuilder().withVertsPerPoly(3).build());
        MeshData meshData = NavMeshBuilder.createNavMeshData(new NavMeshDataCreateParamsBuilder(new RecastBuilder().build(new GeometryProviderBuilder(worldMap).build(), bcfg)).build(bcfg));
        navMesh = new NavMesh(meshData, bcfg.cfg.maxVertsPerPoly, 0);

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
                // Clicked on the map, so build a path to:
                System.out.println(getLocationOnMap());
            }
        });
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
        g.move(0f, 0.5f, 0f);
        gDetailed.move(0f, 1f, 0f);

        rootNode.attachChild(g);
        rootNode.attachChild(gDetailed);
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
}
