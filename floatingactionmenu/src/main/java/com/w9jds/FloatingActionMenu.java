package com.w9jds;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;

import com.w9jds.floatingactionmenu.R;

import java.util.ArrayList;
import java.util.List;

public class FloatingActionMenu extends ViewGroup {

    private static final long ANIMATION_DURATION = 300;
    private static final String TAG = "FloatingActionMenu";
    private static final TimeInterpolator DEFAULT_OPEN_INTERPOLATOR = new OvershootInterpolator();
    private static final TimeInterpolator DEFAULT_CLOSE_INTERPOLATOR = new AnticipateInterpolator();

    private FloatingActionButton menuButton;
    private List<FloatingActionButton> menuItems;
//    private List<Button> menuLabels;
    private List<ChildAnimator> itemAnimators;
    private View backgroundView;

    private AnimatorSet openSet = new AnimatorSet();
    private AnimatorSet closeSet = new AnimatorSet();
    private Animator openOverlay;
    private Animator closeOverlay;

    private OnMenuItemClickListener onMenuItemClickListener;
    private OnMenuToggleListener onMenuToggleListener;

    private boolean isOpen;
    private boolean isAnimating;
    private boolean isCloseOnTouchOutside = true;

//    private int childCount;
    private int menuButtonBackground;
    private int menuButtonRipple;
    private int menuButtonSrc;
    private int overlayBackground;
    private int buttonSpacing;
    private int maxButtonWidth;

    private int menuMarginEnd;
    private int menuMarginBottom;

    public FloatingActionMenu(Context context) {
        this(context, null, 0);
    }

    public FloatingActionMenu(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FloatingActionMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray attributes = context.getTheme()
                .obtainStyledAttributes(attrs, R.styleable.FloatingActionMenu, 0, 0);

        try {
            overlayBackground = attributes
                    .getColor(R.styleable.FloatingActionMenu_overlay_color, Color.parseColor("#7F2a3441"));
            buttonSpacing = attributes
                    .getDimensionPixelSize(R.styleable.FloatingActionMenu_item_spacing, dpToPx(context, 8f));
            menuButtonBackground = attributes
                    .getColor(R.styleable.FloatingActionMenu_base_background, Color.RED);
            menuButtonRipple = attributes
                    .getColor(R.styleable.FloatingActionMenu_base_ripple, Color.parseColor("#66ffffff"));
            menuButtonSrc = attributes
                    .getResourceId(R.styleable.FloatingActionMenu_base_src, R.drawable.ic_positive);
            menuMarginEnd = attributes
                    .getDimensionPixelSize(R.styleable.FloatingActionMenu_base_marginEnd, 0);
            menuMarginBottom = attributes
                    .getDimensionPixelSize(R.styleable.FloatingActionMenu_base_marginBottom, 0);
        }
        finally {
            attributes.recycle();
        }

        menuItems = new ArrayList<>();
        itemAnimators = new ArrayList<>();
//        menuLabels = new ArrayList<>();

        menuButton = new FloatingActionButton(getContext());
        menuButton.setSize(FloatingActionButton.SIZE_AUTO);
//        menuButton.setBackgroundTintList(menuButtonBackground);
        menuButton.setRippleColor(menuButtonRipple);
        menuButton.setImageResource(menuButtonSrc);
        menuButton.setOnClickListener(v -> toggle());

        backgroundView = new View(getContext());
        backgroundView.setBackgroundColor(overlayBackground);
        backgroundView.addOnAttachStateChangeListener(new OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                createDefaultIconAnimation();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {

            }
        });

        addViewInLayout(menuButton, -1, new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        addView(backgroundView);
    }

    static int dpToPx(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * scale);
    }

    @Override
    protected void onFinishInflate() {
        bringChildToFront(backgroundView);
        bringChildToFront(menuButton);
        super.onFinishInflate();
    }

    @Override
    public void addView(@NonNull View child, int index, LayoutParams params) {
        super.addView(child, index, params);
        if (child instanceof FloatingActionButton) {
            child.setLayoutParams(params);
            addMenuItem((FloatingActionButton) child);
        }
    }

    public void addMenuItem(FloatingActionButton item) {
        menuItems.add(item);
        itemAnimators.add(new ChildAnimator(item));
//        AppCompatButton button = new AppCompatButton(getContext());
//        button.setText(item.getContentDescription());
//        addView(button);
//        menuLabels.add(button);
//        item.setTag(button);

        item.setOnClickListener(onItemClickListener);
//        button.setOnClickListener(onItemClickListener);
    }

    public void toggle() {
        if (!isOpen) {
            open();
        } else {
            close();
        }
    }

    public void open() {
        startOpenAnimator();
        isOpen = true;
        if (onMenuToggleListener != null) {
            onMenuToggleListener.onMenuToggle(true);
        }
    }

    public void close() {
        startCloseAnimator();
        isOpen = false;
        if (onMenuToggleListener != null) {
            onMenuToggleListener.onMenuToggle(false);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void createOverlayAnimations() {
        WindowManager manager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        int radius = menuButton.getHeight() / 2;

        closeOverlay = ViewAnimationUtils.createCircularReveal(backgroundView,
                menuButton.getLeft() + radius, menuButton.getTop() + radius, Math.max(size.x, size.y),
                radius);
        closeOverlay.setDuration(500);
        closeOverlay.setInterpolator(new AccelerateDecelerateInterpolator());
        closeOverlay.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                backgroundView.setVisibility(GONE);
                animation.end();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        openOverlay = ViewAnimationUtils.createCircularReveal(backgroundView,
                menuButton.getLeft() + radius, menuButton.getTop() + radius, radius,
                Math.max(size.x, size.y));
        openOverlay.setDuration(500);
        openOverlay.setInterpolator(new AccelerateDecelerateInterpolator());
        openOverlay.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                backgroundView.setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                animation.end();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

    protected void startCloseAnimator() {
        if (closeOverlay == null) {
            createOverlayAnimations();
        }

        closeSet.start();
        closeOverlay.start();
        for (ChildAnimator anim : itemAnimators) {
            anim.startCloseAnimator();
        }
    }

    protected void startOpenAnimator() {
        createOverlayAnimations();

        openOverlay.start();
        openSet.start();
        for (ChildAnimator anim : itemAnimators) {
            anim.startOpenAnimator();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int width;
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int height;
        final int count = getChildCount();
        maxButtonWidth = 0;

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
        }

        for (int i = 0; i < menuItems.size(); i++) {
            FloatingActionButton fab = menuItems.get(i);
//            Button label = menuLabels.get(i);
//            maxButtonWidth = Math.max(maxButtonWidth, label.getMeasuredWidth() + fab.getMeasuredWidth()
//                    + fab.getPaddingEnd() + fab.getPaddingStart());
        }

        maxButtonWidth = Math.max(menuButton.getMeasuredWidth(), maxButtonWidth);

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else {
            width = maxButtonWidth;
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else {
            int heightSum = 0;
            for (int i = 0; i < count; i++) {
                View child = getChildAt(i);
                heightSum += child.getMeasuredHeight() + child.getPaddingBottom();
            }
            height = heightSum;
        }

        setMeasuredDimension(resolveSize(width, widthMeasureSpec),
                resolveSize(height, heightMeasureSpec));
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (isCloseOnTouchOutside) {
            return mGestureDetector.onTouchEvent(event);
        } else {
            return super.onTouchEvent(event);
        }
    }

    GestureDetector mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            return isCloseOnTouchOutside && isOpened();
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            close();
            return true;
        }
    });

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed) {
            backgroundView.layout(l, 0, r, b);

            int buttonsHorizontalCenter = r - l - menuButton.getMeasuredWidth() / 2 - getPaddingRight() - menuMarginEnd;
            int menuButtonTop = b - t - menuButton.getMeasuredHeight() - getPaddingBottom();
            int menuButtonLeft = buttonsHorizontalCenter - menuButton.getMeasuredWidth() / 2;

            menuButton.layout(menuButtonLeft, menuButtonTop - menuMarginBottom,
                    menuButtonLeft + menuButton.getMeasuredWidth(),
                    menuButtonTop + menuButton.getMeasuredHeight() - menuMarginBottom);

            int nextY = menuButtonTop - menuMarginBottom;

            int itemCount = menuItems.size();
            for (int i = 0; i < itemCount; i++) {
                FloatingActionButton item = menuItems.get(i);

                if (item.getVisibility() != GONE) {

                    int childX = buttonsHorizontalCenter - item.getMeasuredWidth() / 2;
                    int childY = nextY - item.getMeasuredHeight() - buttonSpacing;

                    item.layout(childX, childY, childX + item.getMeasuredWidth(), childY + item.getMeasuredHeight());

//                View label = (View) fab.getTag(R.id.fab_label);
//                if (label != null) {
//                    int labelsOffset = (mUsingMenuLabel ? mMaxButtonWidth / 2 : fab.getMeasuredWidth() / 2) + mLabelsMargin;
//                    int labelXNearButton = mLabelsPosition == LABELS_POSITION_LEFT
//                            ? buttonsHorizontalCenter - labelsOffset
//                            : buttonsHorizontalCenter + labelsOffset;
//
//                    int labelXAwayFromButton = mLabelsPosition == LABELS_POSITION_LEFT
//                            ? labelXNearButton - label.getMeasuredWidth()
//                            : labelXNearButton + label.getMeasuredWidth();
//
//                    int labelLeft = mLabelsPosition == LABELS_POSITION_LEFT
//                            ? labelXAwayFromButton
//                            : labelXNearButton;
//
//                    int labelRight = mLabelsPosition == LABELS_POSITION_LEFT
//                            ? labelXNearButton
//                            : labelXAwayFromButton;
//
//                    int labelTop = childY - mLabelsVerticalOffset + (fab.getMeasuredHeight()
//                            - label.getMeasuredHeight()) / 2;
//
//                    label.layout(labelLeft, labelTop, labelRight, labelTop + label.getMeasuredHeight());
//
//                    if (!mIsMenuOpening) {
//                        label.setVisibility(INVISIBLE);
//                    }
//                }

                    nextY = childY - buttonSpacing;


                    if (!isAnimating) {
                        if (!isOpen) {
                            item.setTranslationY(menuButton.getTop() - item.getTop());
                            item.setVisibility(GONE);
//                        label.setVisibility(GONE);
                            backgroundView.setVisibility(GONE);
                        } else {
                            item.setTranslationY(0);
                            item.setVisibility(VISIBLE);
//                        label.setVisibility(VISIBLE);
                            backgroundView.setVisibility(VISIBLE);
                        }
                    }
                }
            }

            if (!isAnimating && getBackground() != null) {
                if (!isOpen) {
                    getBackground().setAlpha(0);
                } else {
                    getBackground().setAlpha(0xff);
                }
            }
        }
    }

    private void createDefaultIconAnimation() {
        Animator.AnimatorListener listener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                isAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isAnimating = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                isAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        };

        ObjectAnimator collapseAnimator = ObjectAnimator.ofFloat(menuButton, "rotation", 135f, 0f);
        ObjectAnimator expandAnimator = ObjectAnimator.ofFloat(menuButton, "rotation", 0f, 135f);

        if (Build.VERSION.SDK_INT >= 21) {
            openSet.playTogether(expandAnimator);
            closeSet.playTogether(collapseAnimator);
        }
        else {

            ValueAnimator hideBackgroundAnimator = ObjectAnimator.ofInt(0xff, 0);
            hideBackgroundAnimator.addUpdateListener(animation -> {
                Integer alpha = (Integer) animation.getAnimatedValue();
                getBackground().setAlpha(alpha > 0xff ? 0xff : alpha);
            });
            ValueAnimator showBackgroundAnimator = ObjectAnimator.ofInt(0, 0xff);
            showBackgroundAnimator.addUpdateListener(animation -> {
                Integer alpha = (Integer) animation.getAnimatedValue();
                getBackground().setAlpha(alpha > 0xff ? 0xff : alpha);
            });

            openSet.playTogether(expandAnimator, showBackgroundAnimator);
            closeSet.playTogether(collapseAnimator, hideBackgroundAnimator);
        }

        openSet.setInterpolator(DEFAULT_OPEN_INTERPOLATOR);
        closeSet.setInterpolator(DEFAULT_CLOSE_INTERPOLATOR);

        openSet.setDuration(ANIMATION_DURATION);
        closeSet.setDuration(ANIMATION_DURATION);

        openSet.addListener(listener);
        closeSet.addListener(listener);
    }

    public boolean isOpened() {
        return isOpen;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("instanceState", super.onSaveInstanceState());
        bundle.putBoolean("openState", isOpen);
        return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            isOpen = bundle.getBoolean("openState");
            state = bundle.getParcelable("instanceState");
        }

        super.onRestoreInstanceState(state);
    }

//    @Override
//    protected void onDetachedFromWindow() {
//        super.onDetachedFromWindow();
//    }


//    @Override
//    public void setBackground(Drawable background) {
//        if (background instanceof ColorDrawable) {
//            super.setBackground(background);
//        } else {
//            throw new IllegalArgumentException("floating only support color background");
//        }
//    }

    private OnClickListener onItemClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            closeSet.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {

                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    closeSet.removeListener(this);
                    if (v instanceof FloatingActionButton) {
                        int i = menuItems.indexOf(v);
                        if (onMenuItemClickListener != null) {
                            onMenuItemClickListener.onMenuItemClick(FloatingActionMenu.this, i, (FloatingActionButton) v);
                        }
                    }
//                    else if (v instanceof Button) {
//                        int i = menuLabels.indexOf(v);
//                        if (onMenuItemClickListener != null) {
//                            onMenuItemClickListener.onMenuItemClick(FloatingActionMenu.this, i, menuItems.get(i));
//                        }
//                    }
                }

                @Override
                public void onAnimationCancel(Animator animator) {

                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }
            });
            close();
        }
    };

    class ChildAnimator implements Animator.AnimatorListener {
        private View view;
        private boolean playingOpenAnimator;

        ChildAnimator(View v) {
            v.animate().setListener(this);
            view = v;
        }

        void startOpenAnimator() {
            view.animate()
                .cancel();
            playingOpenAnimator = true;
            view.animate()
                .translationY(0)
                .setInterpolator(DEFAULT_OPEN_INTERPOLATOR)
                .start();
        }

        void startCloseAnimator() {
            view.animate()
                .cancel();
            playingOpenAnimator = false;
            view.animate()
                .translationY((menuButton.getTop() - view.getTop()))
                .setInterpolator(DEFAULT_CLOSE_INTERPOLATOR)
                .start();
        }

        @Override
        public void onAnimationStart(Animator animation) {
            if (playingOpenAnimator) {
                view.setVisibility(VISIBLE);
            }
            else {
//                ((Button) view.getTag()).setVisibility(GONE);
            }
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (!playingOpenAnimator) {
                view.setVisibility(GONE);
            }
            else {
//                ((Button) view.getTag()).setVisibility(VISIBLE);
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {

        }

        @Override
        public void onAnimationRepeat(Animator animation) {

        }
    }

//    public OnMenuToggleListener getOnMenuToggleListener() {
//        return onMenuToggleListener;
//    }
//
//    public void setOnMenuToggleListener(OnMenuToggleListener onMenuToggleListener) {
//        this.onMenuToggleListener = onMenuToggleListener;
//    }
//
//    public OnMenuItemClickListener getOnMenuItemClickListener() {
//        return onMenuItemClickListener;
//    }
//
//    public void setOnMenuItemClickListener(OnMenuItemClickListener onMenuItemClickListener) {
//        this.onMenuItemClickListener = onMenuItemClickListener;
//    }
}
