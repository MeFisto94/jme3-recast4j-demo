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

package com.jme3.recast4j.demo.controls;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;
import org.recast4j.detour.crowd.CrowdAgentParams;

/**
 *
 * @author Robert
 */
public class CrowdAgentControl implements Control, JmeCloneable {

    protected Spatial spatial;
    protected Vector3f target;
    protected CrowdAgentParams ap = null;
    
    @Override
    public void setSpatial(Spatial spatial) {
        if (this.spatial != null && spatial != null && spatial != this.spatial) {
            throw new IllegalStateException(
                    "This control has already been added to a Spatial");
        }

        if (this.getAgentParams() == null) {
            throw new IllegalStateException(
                    "This control must have a CrowdAgentParams object set prior "
                            + "to adding the control to a spatial.");
        }
        
        this.spatial = spatial;
    }

    @Override
    public void update(float tpf) {
        
    }

    @Override
    public void render(RenderManager rm, ViewPort vp) {

    }

    @Override
    public Control cloneForSpatial(Spatial spatial) {
        throw new UnsupportedOperationException("Do not use cloneForSpatial.");
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Object jmeClone() {
        CrowdAgentControl cac = new CrowdAgentControl();
        
        cac.setTarget(this.getTarget());
        return cac;
    }

    @Override
    public void cloneFields(Cloner cloner, Object original) {
    }

    /**
     * @return the target
     */
    public Vector3f getTarget() {
        return target;
    }

    /**
     * @param target the target to set
     */
    public void setTarget(Vector3f target) {
        this.target = target;
    }

    /**
     * @return the agent parameters object used for crowd navigation.
     */
    public CrowdAgentParams getAgentParams() {
        return ap;
    }

    /**
     * @param ap the agent parameters object to set for crowd navigation.
     */
    public void setAgentParams(CrowdAgentParams ap) {
        this.ap = ap;
    }


}

