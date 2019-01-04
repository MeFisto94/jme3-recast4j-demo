package com.jme3.recast4j.demo;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.control.AbstractControl;

import java.util.ArrayList;
import java.util.List;

public class NavMeshChaserControl extends AbstractControl {
    protected BetterCharacterControl characterControl;
    protected List<Vector3f> pathList;
    protected int currentIndex;
    protected AnimChannel walkChannel;
    /**
     * Epsilon is the distance required to accept a waypoint as close enough, see how changing this value changes
     * walking behavior. Must be >= 0.2, because for some reason the height is always reported as 1.2 but Jaime walks
     * on the height of 1.0
     */
    protected static final float epsilon = 0.3f;
    protected static final float walkspeed = 2f;

    public NavMeshChaserControl() {
        this.pathList = new ArrayList<>();
    }

    @Override
    protected void controlUpdate(float tpf) {
        if (characterControl == null) {
            characterControl = getSpatial().getControl(BetterCharacterControl.class);
        }

        if (characterControl == null) {
            /* While we could automatically use .move() without BCC, it's a potential source of bugs as people might
             * want a Character but forgot about the Control.
             */
            throw new IllegalStateException("Cannot be used without a BetterCharacterControl");
        }

        if (walkChannel == null && getSpatial().getControl(AnimControl.class) != null) {
            walkChannel = getSpatial().getControl(AnimControl.class).createChannel();
        }

        // e.g. index 2 -> size >= 3
        if (pathList.size() >= currentIndex + 1) {
            if (getSpatial().getWorldTranslation().distance(pathList.get(currentIndex)) < epsilon) {
                // reached a target, increase the index, that's all
                currentIndex++;

                if (pathList.size() >= currentIndex + 1) { // still in the list?
                    moveToWaypoint();
                } else { // reached our target
                    stopFollowing();
                }
            } // else -> AntiStuck Detection?
        } // else we've reached our goal
    }

    private void moveToWaypoint() {
        Vector3f dir = pathList.get(currentIndex).subtract(spatial.getWorldTranslation()).setY(0f).normalizeLocal();
        System.out.println("Approaching " + pathList.get(currentIndex) + " Direction: " + dir);
        characterControl.setViewDirection(dir);
        characterControl.setWalkDirection(dir.multLocal(walkspeed));
        walk(true);
    }

    public void followPath(List<Vector3f> pathList) {
        this.pathList = pathList;
        currentIndex = 0;
        if (!pathList.isEmpty()) {
            moveToWaypoint(); // Start walking for the first time
        }
    }

    public void stopFollowing() {
        System.out.println("Stop Walking");
        characterControl.setWalkDirection(Vector3f.ZERO);
        walk(false);
        pathList.clear();
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) { }

    public void walk(boolean walking) {
        if (walkChannel != null) {
            if (walking) {
                walkChannel.setAnim("Walk");
            } else {
                walkChannel.reset(true);
            }
        }
    }

}
