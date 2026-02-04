package com.example.konspect.adapters;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.konspect.R;
import com.example.konspect.models.Conspect;

import java.util.List;

public class ConspectAdapter extends RecyclerView.Adapter<ConspectAdapter.ConspectViewHolder> {
    private List<Conspect> conspects;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Conspect conspect);
    }

    public ConspectAdapter(List<Conspect> conspects) {
        this.conspects = conspects;
    }

    public void setConspects(List<Conspect> conspects) {
        this.conspects = conspects;
        notifyDataSetChanged();
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ConspectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conspect, parent, false);
        return new ConspectViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ConspectViewHolder holder, int position) {
        Conspect conspect = conspects.get(position);

        holder.titleTextView.setText(conspect.getTitle());
        holder.subjectTextView.setText("Предмет: " + conspect.getSubject());

        String content = conspect.getContent();
        String previewText = "";
        if (content != null && !content.isEmpty()) {
            previewText = (content.length() > 100) ? content.substring(0, 100) + "..." : content;
        }
        holder.previewTextView.setText(previewText);

        holder.dateTextView.setText(String.valueOf(conspect.getFormattedDate()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(conspect);
            }
        });
    }

    @Override
    public int getItemCount() {
        return conspects == null ? 0 : conspects.size();
    }

    static class ConspectViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView subjectTextView;
        TextView previewTextView;
        TextView dateTextView;

        public ConspectViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.ttl_card);
            subjectTextView = itemView.findViewById(R.id.subj);
            previewTextView = itemView.findViewById(R.id.preview_consp);
            dateTextView = itemView.findViewById(R.id.date);
        }
    }
}

