package org.levimc.launcher.ui.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.view.animation.AlphaAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;

import org.levimc.launcher.databinding.ActivitySplashBinding;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends BaseActivity {

    private ActivitySplashBinding binding;
    private final Runnable navigateRunnable = new Runnable() {
        @Override
        public void run() {
            Intent newIntent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(newIntent);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        applyTextGradient(binding.tvAppName);
        startLeafAnimation();
        startAppNameAnimation();

        binding.getRoot().postDelayed(navigateRunnable, 2000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (binding != null) {
            binding.getRoot().removeCallbacks(navigateRunnable);
        }
    }

    private void startLeafAnimation() {
        TranslateAnimation translateAnimation = new TranslateAnimation(0, 0, -600, 0);
        translateAnimation.setDuration(1500);
        translateAnimation.setFillAfter(true);
        translateAnimation.setInterpolator(this, android.R.anim.bounce_interpolator);
        binding.imgLeaf.startAnimation(translateAnimation);
    }

    private void applyTextGradient(TextView textView) {
        int startColor = Color.parseColor("#7BAAF7");
        int endColor = Color.parseColor("#B287F7");
        Shader shader = new LinearGradient(
                0, 0, textView.getPaint().measureText(textView.getText().toString()), 0,
                startColor, endColor, Shader.TileMode.CLAMP
        );
        textView.getPaint().setShader(shader);
    }

    private void startAppNameAnimation() {
        AlphaAnimation alphaAnimation = new AlphaAnimation(0f, 1f);
        alphaAnimation.setDuration(2000);
        alphaAnimation.setFillAfter(true);
        binding.tvAppName.startAnimation(alphaAnimation);
    }
}