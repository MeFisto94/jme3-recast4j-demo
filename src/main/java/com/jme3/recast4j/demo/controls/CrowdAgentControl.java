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

/**
 *
 * @author Robert
 */
public class CrowdAgentControl implements Control, JmeCloneable {

    private Spatial spatial;
    private int flags;
    private int obstacleAvoidanceType;
    private Vector3f target;
    
    @Override
    public void setSpatial(Spatial spatial) {
        if (this.spatial != null && spatial != null && spatial != this.spatial) {
            throw new IllegalStateException(
                    "This control has already been added to a Spatial");
        }
        this.spatial = spatial;

    }

    @Override
    public void update(float tpf) {
        
    }

    @Override
    public void render(RenderManager rm, ViewPort vp) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Control cloneForSpatial(Spatial spatial) {
        throw new UnsupportedOperationException("Do not use cloneForSpatial."); //To change body of generated methods, choose Tools | Templates.
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
        cac.setFlags(this.getFlags());
        cac.setObstacleAvoidanceType(this.getObstacleAvoidanceType());
        cac.setTarget(this.getTarget());
        return cac;
    }

    @Override
    public void cloneFields(Cloner cloner, Object original) {
    }

    /**
     * @return the flags
     */
    public int getFlags() {
        return flags;
    }

    /**
     * @param flags the flags to set
     */
    public void setFlags(int flags) {
        this.flags = flags;
    }

    /**
     * @return the obstacleAvoidanceType
     */
    public int getObstacleAvoidanceType() {
        return obstacleAvoidanceType;
    }

    /**
     * @param obstacleAvoidanceType the obstacleAvoidanceType to set
     */
    public void setObstacleAvoidanceType(int obstacleAvoidanceType) {
        this.obstacleAvoidanceType = obstacleAvoidanceType;
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


}

