package com.dataxy;

import androidx.test.espresso.IdlingResource;

final class DataXYSenderIdlingResource implements IdlingResource {
    private ResourceCallback mResourceCallback;

    private int mCounter;
    private int mTargetCount;

    DataXYSenderIdlingResource(int targetCount) {
        mTargetCount = targetCount;
    }

    void increaseCounter() {
        mCounter++;
    }

    @Override
    public String getName() {
        return "DataXYSenderIdlingResource[" + mCounter + "/" + mTargetCount + "]";
    }

    @Override
    public boolean isIdleNow() {
        if (mCounter != mTargetCount) {
            return false;
        }

        mResourceCallback.onTransitionToIdle();
        return true;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback callback) {
        this.mResourceCallback = callback;
    }
}
