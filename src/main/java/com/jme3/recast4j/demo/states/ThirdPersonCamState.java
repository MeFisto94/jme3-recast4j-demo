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
import com.jme3.bounding.BoundingBox;
import com.jme3.input.CameraInput;
import com.jme3.input.ChaseCamera;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Robert
 */
public class ThirdPersonCamState extends BaseAppState {
    
    private static final Logger LOG = LoggerFactory.getLogger(ThirdPersonCamState.class.getName());

    @Override
    protected void initialize(Application app) {
        addHeadNode((Node) ((SimpleApplication) app).getRootNode().getChild("player"));
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
}
