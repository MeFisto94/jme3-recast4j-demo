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
import com.jme3.asset.AssetKey;
import com.jme3.bounding.BoundingBox;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.input.CameraInput;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import org.recast4j.detour.NavMeshDataCreateParams;
import org.recast4j.detour.NavMeshParams;
import org.recast4j.recast.RecastBuilderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Robert
 */
public class RecastGUIState extends BaseAppState {
    
    Logger LOG = LoggerFactory.getLogger(RecastGUIState.class);
    private final Node character;
    private final String sep = File.separator;
    private final Path exportPath = Paths.get("assets" + sep + "Models" + sep + "Level");
    private final String exportFilename = exportPath + sep + "navmesh.j3o";
    private final NavMeshDataCreateParams build;
    private final RecastBuilderConfig bcfg;
    
    public RecastGUIState(Node character, NavMeshDataCreateParams build, RecastBuilderConfig bcfg) {
        this.character = character;
        this.build = build;
        this.bcfg = bcfg;
    }
   
    @Override
    protected void initialize(Application app) {
        addHeadNode(character);
                
        NavMeshDataParameters params = new NavMeshDataParameters();
        params.verts = Arrays.copyOf(build.verts, build.verts.length);
        params.vertCount = build.vertCount;
        params.polys = Arrays.copyOf(build.polys, build.polys.length);
        params.polyAreas = Arrays.copyOf(build.polyAreas, build.polyAreas.length);
        params.polyFlags = Arrays.copyOf(build.polyFlags, build.polyFlags.length);
        params.polyCount = build.polyCount;
        params.nvp = build.nvp;
        params.detailMeshes = Arrays.copyOf(build.detailMeshes, build.detailMeshes.length);
        params.detailVerts = Arrays.copyOf(build.detailVerts, build.detailVerts.length);
        params.detailVertsCount = build.detailVertsCount;
        params.detailTris = Arrays.copyOf(build.detailTris, build.detailTris.length);
        params.detailTriCount = build.detailTriCount;
        params.walkableHeight = build.walkableHeight;
        params.walkableRadius = build.walkableRadius;
        params.walkableClimb = build.walkableClimb;
        params.bmin = Arrays.copyOf(build.bmin, build.bmin.length);
        params.bmax = Arrays.copyOf(build.bmax, build.bmax.length);
        params.cs = build.cs;
        params.ch = build.ch;
        params.buildBvTree = build.buildBvTree;
        
        //create object to save solo NavMesh parameters
        MeshParameters meshParams = new MeshParameters();
        meshParams.addMeshDataParameters(params);
        
        //save NavMesh parameters as .j3o
//        saveNavMesh(meshParams, exportFilename);
        
        showConfig(bcfg);
//        params.printResults();
//        showDebugMesh("Models/Level/navmesh.j3o", ColorRGBA.Green);
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
        //Called when the state is fully enabled, ie: is attached and 
        //isEnabled() is true or when the setEnabled() status changes after the 
        //state is attached.
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
    
    //create 3rd person view.
    private void addHeadNode(Node body) {
        
        BoundingBox bounds = (BoundingBox) body.getWorldBound();
        Node head = new Node("headNode");
        body.attachChild(head);
        
        //offset head node using spatial bounds to pos head level
        head.setLocalTranslation(0, bounds.getYExtent() * 2, 0);
        
        Camera cam = getApplication().getCamera();
        cam.setLocation(new Vector3f(0f, 0f, 10f));
        cam.lookAtDirection(new Vector3f(0f, 0f, 0f), Vector3f.UNIT_Y);
        
        //use offset head node as target for cam to follow
        ChaseCamera chaseCam = new ChaseCamera(cam, head, getApplication().getInputManager());
        
        //duplicate blender rotation
        chaseCam.setInvertVerticalAxis(true);
        
        //disable so camera stays same distance from head when moving
        chaseCam.setSmoothMotion(false);
        
        chaseCam.setDefaultHorizontalRotation(1.57f);
        chaseCam.setRotationSpeed(4f);
        chaseCam.setMinDistance(bounds.getYExtent() * 2);
        chaseCam.setDefaultDistance(10);
        chaseCam.setMaxDistance(25);
        
        //prevent camera rotation below head
        chaseCam.setDownRotateOnCloseViewOnly(false);  
        
        //Set arrow keys to rotate view.
        //Uses default mouse scrolling to zoom.
        chaseCam.setToggleRotationTrigger(
                new KeyTrigger(KeyInput.KEY_LEFT),
                new KeyTrigger(KeyInput.KEY_RIGHT),
                new KeyTrigger(KeyInput.KEY_UP),
                new KeyTrigger(KeyInput.KEY_DOWN));
        
        getApplication().getInputManager().addMapping(
                CameraInput.CHASECAM_MOVERIGHT, new KeyTrigger(KeyInput.KEY_RIGHT));
        getApplication().getInputManager().addMapping(
                CameraInput.CHASECAM_MOVELEFT, new KeyTrigger(KeyInput.KEY_LEFT));
        getApplication().getInputManager().addMapping(
                CameraInput.CHASECAM_DOWN, new KeyTrigger(KeyInput.KEY_DOWN));
        getApplication().getInputManager().addMapping(
                CameraInput.CHASECAM_UP, new KeyTrigger(KeyInput.KEY_UP));
    }
    
        //Exports the Recast NavMesh to assets in .j3o format so can load a 
    //saved Recast NavMesh rather than building.
    private void saveNavMesh(MeshParameters params, String filename) {
        BinaryExporter exporter = BinaryExporter.getInstance();
        File file = new File(filename);
        try {
            exporter.save(params, file);
        } catch (IOException ex) {
            LOG.error("Error: Failed to save recastMesh!\n {}", ex);
        }
    }
    
    private void showDebugMesh(String fileName, ColorRGBA color) {
        Node navmesh = (Node) getApplication().getAssetManager().loadModel(fileName);
        Material mat = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        mat.getAdditionalRenderState().setWireframe(true);
        navmesh.setMaterial(mat);
        navmesh.setCullHint(CullHint.Never);
        ((SimpleApplication) getApplication()).getRootNode().attachChild(navmesh);
    }
    
    public void showMesh(String filename, ColorRGBA color) {
        AssetKey<Mesh> keyLoc = new AssetKey<>(filename);    
        final Mesh meshGeom = (Mesh) getApplication().getAssetManager().loadAsset(keyLoc);
        Geometry geom = new Geometry();
        geom.setMesh(meshGeom);
        Material mat = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        mat.getAdditionalRenderState().setWireframe(true);
        geom.setMaterial(mat);
        geom.setCullHint(CullHint.Never);
        ((SimpleApplication) getApplication()).getRootNode().attachChild(geom);
    }
    
    private void showConfig(RecastBuilderConfig bcfg) {
        LOG.info("<===== CONFIG BUILDER =====>");
        LOG.info("Cell Size             [{} wu]", bcfg.cfg.cs);
        LOG.info("Cell Height           [{} wu]", bcfg.cfg.ch);
        LOG.info("walkableRadius        [{} vx]", bcfg.cfg.walkableRadius);
        LOG.info("walkableHeight        [{} vx]", bcfg.cfg.walkableHeight);
        LOG.info("walkableClimb         [{} vx]", bcfg.cfg.walkableClimb);
        LOG.info("agentMaxSlope         [{} deg]", bcfg.cfg.walkableSlopeAngle);
        LOG.info("maxEdgeLen            [{} vx]", bcfg.cfg.maxEdgeLen);
        LOG.info("edgeMaxError          [{} vx]", bcfg.cfg.maxSimplificationError);
        LOG.info("detailSampleDist      [{} wu]", bcfg.cfg.detailSampleDist);
        LOG.info("detailSampleMaxError  [{} wu]", bcfg.cfg.detailSampleMaxError);
        LOG.info("<===== CONFIG BUILDER =====>");
    }
        
    /**
     * Used to save the NavMeshParams in .j3o format.
     * @author mitm
     */
    public class NavMeshParameters extends NavMeshParams implements Savable {

        @Override
        public void write(JmeExporter ex) throws IOException {
            OutputCapsule capsule = ex.getCapsule(this);
            capsule.write(orig, "origin", null);
            capsule.write(tileWidth, "tilewidth", 0.0f);
            capsule.write(tileHeight, "tileheight", 0.0f);
            capsule.write(maxTiles, "maxtiles", 0);
            capsule.write(maxPolys, "maxpolys", 0);
        }

        @Override
        public void read(JmeImporter im) throws IOException {
            InputCapsule capsule = im.getCapsule(this);
            float[] origin = capsule.readFloatArray("origin", null);
            System.arraycopy(origin, 0, orig, 0, origin.length);      
            tileWidth = capsule.readFloat("tilewidth", 0.0f);        
            tileHeight = capsule.readFloat("tileheight", 0.0f);        
            maxTiles = capsule.readInt("maxtiles", 0);        
            maxPolys = capsule.readInt("maxpolys", 0);        
        }

        /**
         * Prints all saved varibles of the NavMeshParams class.
         */
        public void printResults() {
            LOG.info("<==========NavMeshParams==========>");
            LOG.info("origin        [{}]", Arrays.toString(orig));
            LOG.info("tileWidth     [{}]", tileWidth);
            LOG.info("tileHeight    [{}]", tileHeight);
            LOG.info("maxTiles      [{}]", maxTiles);
            LOG.info("maxPolys      [{}]", maxPolys);
            LOG.info("<========== NavMeshParams ==========>");
        }
    }
        
    /**
     * Used to save the NavMeshDataCreateParams in .j3o format.
     * @author mitm
     */
    private class NavMeshDataParameters extends NavMeshDataCreateParams implements Savable {

        @Override
        public void write(JmeExporter ex) throws IOException {
            OutputCapsule capsule = ex.getCapsule(this);
            capsule.write(verts, "verticies",   null);
            capsule.write(vertCount, "vertcount", 6);
            capsule.write(polys, "polygons", null);
            capsule.write(polyFlags, "polyflags", null);
            capsule.write(polyAreas, "polyareas", null);
            capsule.write(polyCount, "polycount", 0);
            capsule.write(nvp, "vertsperpoly", 6);

            capsule.write(detailMeshes, "detailmeshes", null);
            capsule.write(detailVerts, "detailverts", null);
            capsule.write(detailVertsCount, "detailvertscount", 0);
            capsule.write(detailTris, "detailtris", null);
            capsule.write(detailTriCount, "detailtricount", 0);

    //        capsule.write(userId, "userid", 0);
            capsule.write(tileX, "tilex", 0);
            capsule.write(tileY, "tiley", 0);
    //        capsule.write(tileLayer, "tilelayer", 0);
            capsule.write(bmin, "bMin", null);
            capsule.write(bmax, "bMax", null);

            capsule.write(walkableHeight, "walkableheight", 0.0f);
            capsule.write(walkableRadius, "walkableradius", 0.0f);
            capsule.write(walkableClimb, "walkableclimb", 0.0f);
            capsule.write(cs, "cellsize", 0.0f);
            capsule.write(ch, "cellheight", 0.0f);
            capsule.write(buildBvTree, "buildbvtree", true);
        }

        @Override
        public void read(JmeImporter im) throws IOException {
            InputCapsule capsule = im.getCapsule(this);
            verts = capsule.readIntArray("verticies", null);  
            vertCount = capsule.readInt("vertcount", 6);        
            polys = capsule.readIntArray("polygons", null);        
            polyFlags = capsule.readIntArray("polyflags", null);        
            polyAreas = capsule.readIntArray("polyareas", null);        
            polyCount = capsule.readInt("polycount", 0);        
            nvp = capsule.readInt("vertsperpoly", 6); 

            detailMeshes = capsule.readIntArray("detailmeshes", null);        
            detailVerts = capsule.readFloatArray("detailverts", null);        
            detailVertsCount = capsule.readInt("detailvertscount", 0);        
            detailTris = capsule.readIntArray("detailtris", null);        
            detailTriCount = capsule.readInt("detailtricount", 0); 

    //        userId = capsule.readInt("userid", 0);
            tileX = capsule.readInt("tilex", 0);        
            tileY = capsule.readInt("tiley", 0);        
    //        tileLayer = capsule.readInt("tilelayer", 0);
            bmin = capsule.readFloatArray("bMin", null);
            bmax = capsule.readFloatArray("bMax", null);

            walkableHeight = capsule.readFloat("walkableheight", 0.0f);
            walkableRadius = capsule.readFloat("walkableradius", 0.0f);        
            walkableClimb = capsule.readFloat("walkableclimb", 0.0f);        
            cs = capsule.readFloat("cellsize", 0.0f);        
            ch = capsule.readFloat("cellheight", 0.0f);        
            buildBvTree = capsule.readBoolean("buildbvtree", true);        
        }

        /**
         * Prints all saved varibles of the NavMeshDataCreateParams class.
         */
        public void printResults() {
            LOG.info("<===== NavMeshDataParameters =====>");
            LOG.info("verts              {}", Arrays.toString(verts));
            LOG.info("vertCount         [{}]", vertCount);
            LOG.info("polys              {}", Arrays.toString(polys));
            LOG.info("polyFlags          {}", Arrays.toString(polyFlags));
            LOG.info("polyAreas          {}", Arrays.toString(polyAreas));
            LOG.info("polyCount         [{}]", polyCount);
            LOG.info("nvp               [{}]", nvp);
            LOG.info("detailMeshes       {}", Arrays.toString(detailMeshes));
            LOG.info("detailVerts        {}", Arrays.toString(detailVerts));
            LOG.info("detailVertsCount  [{}]", detailVertsCount);
            LOG.info("detailTris         {}", Arrays.toString(detailTris));
            LOG.info("detailTriCount    [{}]", detailTriCount);
//            LOG.info("userId            [{}]", userId);
            LOG.info("tileX             [{}]", tileX);
            LOG.info("tileY             [{}]", tileY);
//            LOG.info("tileLayer         [{}]", tileLayer);
            LOG.info("bmin               {}", Arrays.toString(bmin));
            LOG.info("bmax               {}", Arrays.toString(bmax));
            LOG.info("walkableHeight    [{}]", walkableHeight);
            LOG.info("walkableRadius    [{}]", walkableRadius);
            LOG.info("walkableClimb     [{}]", walkableClimb);
            LOG.info("cs                [{}]", cs);
            LOG.info("ch                [{}]", ch);
            LOG.info("buildBvTree       [{}]", buildBvTree);
            LOG.info("<===== NavMeshDataParameters =====>");
        }
    
    }
    
    /**
     *
     * @author mitm
     */
    private class MeshParameters implements Savable {

        private ArrayList<NavMeshParameters> navMeshList;
        private ArrayList<NavMeshDataParameters> navMeshDataList;

        public MeshParameters() {
            this.navMeshList = new ArrayList<>();
            this.navMeshDataList = new ArrayList<>();
        }

        public void addMeshParameters(NavMeshParameters navMeshParams) {
            this.navMeshList.add(navMeshParams);
        }

        public NavMeshParameters getNavMeshParameters() {
            return navMeshList.get(0);
        }

        public void addMeshDataParameters(NavMeshDataParameters navMeshDataParams) {
            this.navMeshDataList.add(navMeshDataParams);
        }

        public NavMeshDataParameters getNavMeshDataParameters(int index) {
            return navMeshDataList.get(index);
        }

        public int getNumTiles() {
            return navMeshDataList.size();
        }

        @Override
        public void write(JmeExporter ex) throws IOException {
            OutputCapsule capsule = ex.getCapsule(this);
            capsule.writeSavableArrayList(navMeshList, "navmeshlist", null);
            capsule.writeSavableArrayList(navMeshDataList, "navmeshdatalist", null);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void read(JmeImporter im) throws IOException {
            InputCapsule capsule = im.getCapsule(this);
            navMeshList = (ArrayList<NavMeshParameters>) capsule.readSavableArrayList("navmeshlist", new ArrayList<>());
            navMeshDataList = (ArrayList<NavMeshDataParameters>) capsule.readSavableArrayList("navmeshdatalist", new ArrayList<>());
        }

    }

}
