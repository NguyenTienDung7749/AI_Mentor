package com.example.aimentor.adapters;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
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
        List<Question> replacement = newItems == null
                ? new ArrayList<>() : new ArrayList<>(newItems);
        DiffUtil.DiffResult changes = DiffUtil.calculateDiff(
                new DiffUtil.Callback() {
                    @Override
                    public int getOldListSize() {
                        return items.size();
                    }

                    @Override
                    public int getNewListSize() {
                        return replacement.size();
                    }

                    @Override
                    public boolean areItemsTheSame(int oldPosition, int newPosition) {
                        return items.get(oldPosition).id == replacement.get(newPosition).id;
                    }

                    @Override
                    public boolean areContentsTheSame(int oldPosition, int newPosition) {
                        Question oldItem = items.get(oldPosition);
                        Question newItem = replacement.get(newPosition);
                        return oldItem.bookmarked == newItem.bookmarked
                                && safe(oldItem.questionText).equals(safe(newItem.questionText))
                                && safe(oldItem.subject).equals(safe(newItem.subject))
                                && safe(oldItem.answerSource).equals(safe(newItem.answerSource))
                                && oldItem.createdAt == newItem.createdAt;
                    }
                });
        items.clear();
        items.addAll(replacement);
        changes.dispatchUpdatesTo(this);
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
        holder.tvMeta.setText(holder.itemView.getContext().getString(
                R.string.history_item_meta, q.subject, source, when));
        holder.tvBookmark.setText(q.bookmarked
                ? R.string.bookmarked_symbol : R.string.bookmark_symbol);
        holder.tvBookmark.setContentDescription(holder.itemView.getContext().getString(
                q.bookmarked ? R.string.remove_bookmark_description
                        : R.string.bookmark_question_description,
                q.questionText));
        holder.itemView.setContentDescription(holder.itemView.getContext().getString(
                R.string.open_saved_answer_description, q.questionText));

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

    private static String safe(String value) {
        return value == null ? "" : value;
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
