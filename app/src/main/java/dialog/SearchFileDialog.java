package dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.guhh.sopmaster.R;

/**
 * Created by sunpn on 2017/9/5.
 */

public class SearchFileDialog extends Dialog{
    private TextView msg_tv;
    public SearchFileDialog(Context context) {
        super(context);
    }

    public SearchFileDialog(Context context, int theme) {
        super(context, theme);
    }

//    /**
//     * 当窗口焦点改变时调用
//     */
//    public void onWindowFocusChanged(boolean hasFocus) {
//        ImageView imageView = (ImageView) findViewById(R.id.spinnerImageView);
//        // 获取ImageView上的动画背景
//        AnimationDrawable spinner = (AnimationDrawable) imageView.getBackground();
//        // 开始动画
//        spinner.start();
//    }


    /**
     * 给Dialog设置提示信息
     *
     * @param message
     */
    public void setMessage(CharSequence message) {
        if (message != null && message.length() > 0 ) {
            msg_tv.setText(message);
            msg_tv.invalidate();
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        msg_tv = (TextView) findViewById(R.id.message);
    }

    /**
     * 弹出自定义ProgressDialog
     *
     * @param context
     *            上下文
     *            按下返回键监听
     * @return
     */
    public static SearchFileDialog show(Context context, boolean cancelable) {
        SearchFileDialog dialog = new SearchFileDialog(context, R.style.Custom_Progress);
        dialog.setTitle("");
        dialog.setContentView(R.layout.dialog_search_file);
        // 按返回键是否取消
        dialog.setCancelable(cancelable);
        // 监听返回键处理
//        dialog.setOnCancelListener(cancelListener);
        // 设置居中
        dialog.getWindow().getAttributes().gravity = Gravity.CENTER;
        WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
        // 设置背景层透明度
        lp.dimAmount = 0.5f;
        dialog.getWindow().setAttributes(lp);
        // dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
        dialog.show();
        return dialog;
    }

//    @Override
//    public void setOnDismissListener(@Nullable OnDismissListener listener) {
//        super.setOnDismissListener(this);
//    }
}
