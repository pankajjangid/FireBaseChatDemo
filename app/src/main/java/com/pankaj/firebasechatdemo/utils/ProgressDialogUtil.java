package com.pankaj.firebasechatdemo.utils;

import android.app.ProgressDialog;
import android.content.Context;

import com.pankaj.firebasechatdemo.R;


public class ProgressDialogUtil {

    private static boolean isLoadingVisible;
    private static ProgressDialog mProgressDialog;
    private Context context;

    public static void showProgress(Context mContext) {
        if (isLoadingVisible) {
            hideProgress();
        }
        isLoadingVisible = true;
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setMessage(mContext.getString(R.string.loading));
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    public static void showProgress(Context mContext,String text) {
        if (isLoadingVisible) {
            hideProgress();
        }
        isLoadingVisible = true;
        mProgressDialog = new ProgressDialog(mContext);
        mProgressDialog.setMessage(text);
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    /**
     * method to hide progress
     */
    public static void hideProgress() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            isLoadingVisible = false;
        }
    }

}
