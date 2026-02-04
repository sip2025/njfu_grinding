package com.examapp.util;
import android.animation.ObjectAnimator;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
public class DraggableFABHelper {
private float dX, dY;
private float initialX, initialY;
private boolean isDragging = false;
private static final float CLICK_THRESHOLD = 10f;
public void makeDraggable(View fab, View.OnClickListener onClickListener) {
fab.setOnTouchListener(new View.OnTouchListener() {
@Override
public boolean onTouch(View v, MotionEvent event) {
ViewGroup parent = (ViewGroup) v.getParent();
switch (event.getAction()) {
case MotionEvent.ACTION_DOWN:
dX = v.getX() - event.getRawX();
dY = v.getY() - event.getRawY();
initialX = event.getRawX();
initialY = event.getRawY();
isDragging = false;
return true;
case MotionEvent.ACTION_MOVE:
float newX = event.getRawX() + dX;
float newY = event.getRawY() + dY;
float deltaX = Math.abs(event.getRawX() - initialX);
float deltaY = Math.abs(event.getRawY() - initialY);
if (deltaX > CLICK_THRESHOLD || deltaY > CLICK_THRESHOLD) {
isDragging = true;
}
if (parent != null) {
newX = Math.max(0, Math.min(newX, parent.getWidth() - v.getWidth()));
newY = Math.max(0, Math.min(newY, parent.getHeight() - v.getHeight()));
}
v.setX(newX);
v.setY(newY);
return true;
case MotionEvent.ACTION_UP:
if (!isDragging) {
if (onClickListener != null) {
onClickListener.onClick(v);
}
} else {
snapToEdge(v, parent);
}
return true;
}
return false;
}
});
}
private void snapToEdge(View view, ViewGroup parent) {
if (parent == null) return;
float viewCenterX = view.getX() + view.getWidth() / 2f;
float viewCenterY = view.getY() + view.getHeight() / 2f;
float parentWidth = parent.getWidth();
float parentHeight = parent.getHeight();
float distanceToLeft = viewCenterX;
float distanceToRight = parentWidth - viewCenterX;
float distanceToTop = viewCenterY;
float distanceToBottom = parentHeight - viewCenterY;
float minDistance = Math.min(
Math.min(distanceToLeft, distanceToRight),
Math.min(distanceToTop, distanceToBottom)
);
float targetX = view.getX();
float targetY = view.getY();
float targetTranslationX = 0f;
if (minDistance == distanceToLeft) {
targetX = -view.getWidth() / 2f;
targetTranslationX = 0f;
} else if (minDistance == distanceToRight) {
targetX = parentWidth - view.getWidth() / 2f;
targetTranslationX = 0f;
} else if (minDistance == distanceToTop) {
targetY = -view.getHeight() / 2f;
} else {
targetY = parentHeight - view.getHeight() / 2f;
}
ObjectAnimator animX = ObjectAnimator.ofFloat(view, "x", targetX);
ObjectAnimator animY = ObjectAnimator.ofFloat(view, "y", targetY);
animX.setDuration(300);
animY.setDuration(300);
animX.setInterpolator(new DecelerateInterpolator());
animY.setInterpolator(new DecelerateInterpolator());
animX.start();
animY.start();
}
}