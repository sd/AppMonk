package appmonk.tricks;

import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.ScaleAnimation;

public class AnimationTricks {

	public static Animation createHeightChangeAnimation(int oldHeight, int newHeight, int duration, AnimationListener listener) {
		float targetScale = (float)newHeight / (float)oldHeight;
		ScaleAnimation anim = new ScaleAnimation(1, 1, 1.0f, targetScale);
		anim.setDuration(duration);
		anim.setAnimationListener(listener);
		return anim;
	}
	
	public static Animation createFadeAnimation(boolean out, int duration, AnimationListener listener) {
		AlphaAnimation anim = new AlphaAnimation((out ? 1.0f : 0.0f), (out ? 0.0f : 1.0f));
		anim.setDuration(duration);
		anim.setAnimationListener(listener);
		return anim;
	}
	
}
