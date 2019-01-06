package com.jme3.recast4j.demo.controls;

public class NavMeshChaserControl extends AbstractNavMeshControl {
    /**
     * Epsilon is the distance required to accept a waypoint as close enough, see how changing this value changes
     * walking behavior. Must be >= 0.2, because for some reason the height is always reported as 1.2 but Jaime walks
     * on the height of 1.0
     */
    protected static final float epsilon = 0.3f;

    public NavMeshChaserControl() {
        super();
    }

    @Override
    protected void controlUpdate(float tpf) {
        super.controlUpdate(tpf);

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
}
