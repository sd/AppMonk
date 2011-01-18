package appmonk.tricks;

import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.Animation.AnimationListener;

public class AnimationTricks {

	public static Animation createHeightChangeAnimation(int oldHeight, int newHeight, int duration, AnimationListener listener) {
		float targetScale = (float)newHeight / (float)oldHeight;
		ScaleAnimation anim = new ScaleAnimation(1, 1, 1.0f, targetScale);
		anim.setDuration(duration);
		anim.setAnimationListener(listener);
		return anim;
	}
	
}
