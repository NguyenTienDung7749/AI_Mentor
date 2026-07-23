package com.example.aimentor.adapters;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aimentor.R;
import com.example.aimentor.ai.AnswerSource;
import com.example.aimentor.data.Question;

import java.util.ArrayList;
import java.util.List;

/** Lists saved questions in the Library tab. */
public class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.VH> {

    public interface Listener {
        void onOpen(Question question);
        void onBookmarkToggle(Question question);
    }

    private final List<Question> items = new ArrayList<>();
    private final Listener listener;

    public QuestionAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<Question> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_question, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Question q = items.get(position);
        holder.tvQuestion.setText(q.questionText);
        CharSequence when = DateUtils.getRelativeTimeSpanString(
                q.createdAt, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS);
        String source = sourceLabel(holder, q);
        holder.tvMeta.setText(q.subject + "  \u2022  " + source + "  \u2022  " + when);
        holder.tvBookmark.setText(q.bookmarked ? "\u2605" : "\u2606");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOpen(q);
        });
        holder.tvBookmark.setOnClickListener(v -> {
            if (listener != null) listener.onBookmarkToggle(q);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String sourceLabel(VH holder, Question question) {
        AnswerSource source = AnswerSource.fromStorage(question.answerSource);
        switch (source) {
            case REMOTE:
                return holder.itemView.getContext().getString(R.string.source_short_online);
            case LOCAL:
                return holder.itemView.getContext().getString(R.string.source_short_offline);
            case LOCAL_FALLBACK:
                return holder.itemView.getContext().getString(R.string.source_short_fallback);
            case CACHE:
                return holder.itemView.getContext().getString(R.string.source_short_cached);
            case LEGACY:
            default:
                return holder.itemView.getContext().getString(R.string.source_short_saved);
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvQuestion, tvMeta, tvBookmark;
        VH(@NonNull View itemView) {
            super(itemView);
            tvQuestion = itemView.findViewById(R.id.tvQuestion);
            tvMeta = itemView.findViewById(R.id.tvMeta);
            tvBookmark = itemView.findViewById(R.id.tvBookmark);
        }
    }
}
