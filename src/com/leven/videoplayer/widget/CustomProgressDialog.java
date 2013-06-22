package com.leven.videoplayer.widget;

import com.leven.videoplayer.R;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;

public class CustomProgressDialog extends Dialog {
    private Context context = null;
    private static CustomProgressDialog pd = null;

    public CustomProgressDialog(Context context, int theme) {
        super(context, theme);
    }

    public CustomProgressDialog(Context context) {
        super(context);
        this.context = context;
    }
    
    public static CustomProgressDialog createDialog(Context context) {
        pd = new CustomProgressDialog(context, R.style.CustomProgressDialog);
        pd.setContentView(R.layout.loading_dialog);
        pd.setCancelable(true);
        pd.getWindow().getAttributes().gravity = Gravity.CENTER;
        return pd;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(pd == null) {
            return;
        }
        ImageView imgIoading = (ImageView) pd.findViewById(R.id.iv_loading);
        AnimationDrawable animationDrawable = (AnimationDrawable) imgIoading.getBackground();
        animationDrawable.start();
    }

    public CustomProgressDialog setMessage(int resId) {
        TextView txtLoading = (TextView) pd.findViewById(R.id.tv_loading);
        txtLoading.setText(resId);
        return pd;
    }
    
}
