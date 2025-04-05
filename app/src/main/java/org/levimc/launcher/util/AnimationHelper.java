package org.levimc.launcher.util;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import org.levimc.launcher.R;
import org.levimc.launcher.databinding.ActivityMainBinding;

public class AnimationHelper {
    public static void prepareInitialStates(ActivityMainBinding binding) {
        binding.header.setVisibility(View.INVISIBLE);
        setViewAnimationState(binding.mainCard, -50f);
        setViewAnimationState(binding.modCard, -30f);
        setViewAnimationState(binding.aboutCard, 30f);
        binding.githubIcon.setAlpha(0f);
    }

    private static void setViewAnimationState(View view, float translationX) {
        view.setAlpha(0f);
        view.setTranslationX(translationX);
    }

    public static void runInitializationSequence(ActivityMainBinding binding) {
        binding.header.postDelayed(() -> startHeaderAnimation(binding.header), 300);
        binding.mainCard.postDelayed(() -> animateView(binding.mainCard, 600, 1.2f), 500);
        binding.modCard.postDelayed(() -> animateView(binding.modCard, 400, 1f), 700);
        binding.aboutCard.postDelayed(() -> animateView(binding.aboutCard, 400, 1f), 800);
        binding.githubIcon.postDelayed(() -> animateGithubIcon(binding.githubIcon), 1000);
    }

    private static void startHeaderAnimation(View header) {
        header.setVisibility(View.VISIBLE);
        header.startAnimation(AnimationUtils.loadAnimation(header.getContext(), R.anim.slide_in_top));
    }

    private static void animateView(View view, int duration, float tension) {
        view.animate().alpha(1f).translationX(0f)
                .setInterpolator(new OvershootInterpolator(tension))
                .setDuration(duration).start();
    }

    private static void animateGithubIcon(View githubIcon) {
        githubIcon.setAlpha(1f);
        ObjectAnimator animator = ObjectAnimator.ofFloat(githubIcon, "translationY", -15f,15f);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setDuration(1500);
        animator.start();
    }
}