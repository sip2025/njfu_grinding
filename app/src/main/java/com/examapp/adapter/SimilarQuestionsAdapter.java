package com.examapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.TypedValue;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.examapp.R;
import com.examapp.model.Question;
import java.util.List;

public class SimilarQuestionsAdapter extends RecyclerView.Adapter<SimilarQuestionsAdapter.ViewHolder> {

    private List<Question> questions;

    public SimilarQuestionsAdapter(List<Question> questions) {
        this.questions = questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_similar_question, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Question question = questions.get(position);

        String type = question.getType();
        if (type == null || type.isEmpty()) {
            type = question.getCategory();
        }
        holder.tvQuestionType.setText(type != null ? type : "题目");
        holder.tvQuestionText.setText(question.getQuestionText());

        holder.layoutOptions.removeAllViews();
        if (question.getOptions() != null) {
            for (String option : question.getOptions()) {
                TextView tvOption = new TextView(holder.itemView.getContext());
                tvOption.setText(option);
                tvOption.setPadding(0, 4, 0, 4);
                // 使用主题文本颜色以支持黑夜模式
                TypedValue typedValue = new TypedValue();
                holder.itemView.getContext().getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
                tvOption.setTextColor(typedValue.data);
                holder.layoutOptions.addView(tvOption);
            }
        }

        holder.tvAnswer.setText("答案: " + question.getAnswer());
    }

    @Override
    public int getItemCount() {
        return questions == null ? 0 : questions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestionType;
        TextView tvQuestionText;
        LinearLayout layoutOptions;
        TextView tvAnswer;

        ViewHolder(View itemView) {
            super(itemView);
            tvQuestionType = itemView.findViewById(R.id.tvQuestionType);
            tvQuestionText = itemView.findViewById(R.id.tvQuestionText);
            layoutOptions = itemView.findViewById(R.id.layoutOptions);
            tvAnswer = itemView.findViewById(R.id.tvAnswer);
        }
    }
}
