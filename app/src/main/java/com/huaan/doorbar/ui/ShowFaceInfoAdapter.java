package com.huaan.doorbar.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.huaan.doorbar.R;
import com.huaan.doorbar.faceserver.CompareResult;
import com.huaan.doorbar.faceserver.FaceServer;

import java.io.File;
import java.util.List;

public class ShowFaceInfoAdapter extends RecyclerView.Adapter<ShowFaceInfoAdapter.CompareResultHolder> {
    private List<CompareResult> compareResultList;
    private LayoutInflater inflater;
    private Context mContext;

    public ShowFaceInfoAdapter(List<CompareResult> compareResultList, Context context) {
        inflater = LayoutInflater.from(context);
        this.compareResultList = compareResultList;
        this.mContext = context;
    }

    @NonNull
    @Override
    public CompareResultHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.item_head, parent, false);
        CompareResultHolder compareResultHolder = new CompareResultHolder(itemView);
        compareResultHolder.textView = itemView.findViewById(R.id.tv_item_name);
        compareResultHolder.imageView = itemView.findViewById(R.id.iv_item_head_img);
        return compareResultHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull CompareResultHolder holder, int position) {
        if (compareResultList == null) {
            return;
        }
        File imgFile = new File(FaceServer.ROOT_PATH + File.separator + FaceServer.SAVE_IMG_DIR + File.separator + compareResultList.get(position).getUserName() + FaceServer.IMG_SUFFIX);
        Glide.with(mContext)
                .load(imgFile)
                .centerCrop()
                .into(holder.imageView);
        holder.textView.setText(compareResultList.get(position).getUserName());
    }

    @Override
    public int getItemCount() {
        return compareResultList == null ? 0 : compareResultList.size();
    }

    class CompareResultHolder extends RecyclerView.ViewHolder {

        TextView textView;
        ImageView imageView;

        CompareResultHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
