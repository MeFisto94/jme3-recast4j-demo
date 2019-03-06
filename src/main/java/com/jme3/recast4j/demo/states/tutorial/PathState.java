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

package com.jme3.recast4j.demo.states.tutorial;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.jme3.recast4j.Detour.DetourUtils;
import static com.jme3.recast4j.Recast.SampleAreaModifications.SAMPLE_POLYFLAGS_DISABLED;
import static com.jme3.recast4j.Recast.SampleAreaModifications.SAMPLE_POLYFLAGS_DOOR;
import static com.jme3.recast4j.Recast.SampleAreaModifications.SAMPLE_POLYFLAGS_WALK;
import com.jme3.scene.Spatial;
import java.util.ArrayList;
import java.util.List;
import org.recast4j.detour.DefaultQueryFilter;
import org.recast4j.detour.FindNearestPolyResult;
import org.recast4j.detour.NavMeshQuery;
import org.recast4j.detour.Result;
import org.recast4j.detour.StraightPathItem;

/**
 * This is purely a non working tutorial example. Used for code demo only.
 * 
 * @author Robert
 */
public class PathState extends BaseAppState {

    private final NavMeshQuery query;
    private Spatial spatial;
    
    public PathState(NavMeshQuery query) {
        this.query = query;
    }
    
    @Override
    protected void initialize(Application app) {
        //It is technically safe to do all initialization and cleanup in the 
        //onEnable()/onDisable() methods. Choosing to use initialize() and 
        //cleanup() for this is a matter of performance specifics for the 
        //implementor.
        //TODO: initialize your AppState, e.g. attach spatials to rootNode
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
        //Use the DefaultQueryFilter so can set flags.        
        DefaultQueryFilter filter = new DefaultQueryFilter();
        
        int includeFlags = SAMPLE_POLYFLAGS_WALK | SAMPLE_POLYFLAGS_DOOR;
        filter.setIncludeFlags(includeFlags);
        
        int excludeFlags = SAMPLE_POLYFLAGS_DISABLED;
        filter.setExcludeFlags(excludeFlags);
        
        //The target.
        Vector3f locOnMap = getLocationOnMap();
        
        //Extents can be anything you determine is appropriate.
        float[] extents = new float[] { 2f, 4f, 2f };
        
        //Spatials current position.
        float[] start = spatial.getWorldTranslation().toArray(null);
        
        //Convert to Recast4j native format.
        float[] end = DetourUtils.toFloatArray(locOnMap);
        
        //Get closet poly for start position.
        FindNearestPolyResult findPolyStart = query.findNearestPoly(start, extents, filter);
        //Get the closest poly for end position.
        FindNearestPolyResult findPolyEnd = query.findNearestPoly(end, extents, filter);
        
        //Get the references for the found polygons.
        long startRef = findPolyStart.getNearestRef();
        long endRef = findPolyEnd.getNearestRef();
        
        //Get the points inside the polygon.
        float[] startPos = findPolyStart.getNearestPos();
        float[] endPos = findPolyEnd.getNearestPos();

        //Get list of polys along the path.
        Result<List<Long>> path = query.findPath(startRef, endRef, startPos, endPos, filter);
        
        //Set the parameters for straight path. Paths cannot exceed 256 polygons.
        int maxStraightPath = 256;
        int options = 0;
        
        //Calculate corners within the path corridor.
        Result<List<StraightPathItem>> pathStr = query.findStraightPath(startPos, endPos, path.result, maxStraightPath, options);
        
        //Provide list for waypoints.
        List<Vector3f> wayPoints = new ArrayList<>(pathStr.result.size());
        
        //Add waypoints to the list
        for (StraightPathItem p: pathStr.result) {
            Vector3f vector = DetourUtils.createVector3f(p.getPos());
            wayPoints.add(vector);
        }
    }

    //Empty method to allow compiling.
    private Vector3f getLocationOnMap() {
        return new Vector3f();
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
    
    
    
}
