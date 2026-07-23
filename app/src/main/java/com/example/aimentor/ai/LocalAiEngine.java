package com.example.aimentor.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Fully offline, deterministic study-mentor engine.
 *
 * It behaves as a coach: it detects the subject and difficulty, computes real
 * results for arithmetic questions, and returns a structured explanation
 * (steps, key concepts, common mistakes and follow-up practice) tailored to the
 * student's education level and preferred explanation style. No network access,
 * API key or backend is required, which keeps the MVP within budget and makes
 * every feature usable offline.
 */
public class LocalAiEngine implements AiEngine {

    @Override
    public String name() {
        return "Local Study Mentor (offline)";
    }

    @Override
    public AiAnswer answer(String question, String educationLevel,
                           String explanationStyle, String subjectHint) {
        AiAnswer a = new AiAnswer();
        a.setSource(AnswerSource.LOCAL);
        a.setModelName(name());
        String q = question == null ? "" : question.trim();

        String subject = (subjectHint != null && !subjectHint.isEmpty()
                && !subjectHint.equalsIgnoreCase("Auto")
                && !subjectHint.equalsIgnoreCase(SubjectClassifier.GENERAL))
                ? subjectHint
                : SubjectClassifier.classify(q);
        a.setSubject(subject);
        a.setDifficulty(difficultyFor(educationLevel));

        String style = explanationStyle == null ? "" : explanationStyle.toLowerCase(Locale.ROOT);
        boolean wantSteps = !style.contains("short");
        boolean wantExtras = style.contains("detail") || style.contains("step") || style.isEmpty();

        // Real computation for bare arithmetic expressions.
        if (subject.equals(SubjectClassifier.MATH) && MathEvaluator.looksLikeExpression(q)) {
            try {
                double result = MathEvaluator.evaluate(q);
                a.setDirectAnswer("The result of " + q.replace("?", "").trim()
                        + " is " + formatNumber(result) + ".");
                if (wantSteps) {
                    a.getSteps().add("Read the expression and note the operators involved.");
                    a.getSteps().add("Apply the order of operations (PEMDAS/BODMAS): "
                            + "brackets, exponents, multiplication and division, then addition and subtraction.");
                    a.getSteps().add("Work left to right within each precedence level.");
                    a.getSteps().add("Final value = " + formatNumber(result) + ".");
                }
                a.setSimplified("Solve the highest-priority operations first, then move down to + and -.");
                if (wantExtras) {
                    a.getKeyConcepts().addAll(Arrays.asList(
                            "Order of operations (PEMDAS/BODMAS)",
                            "Left-to-right evaluation within the same precedence"));
                    a.getCommonMistakes().addAll(Arrays.asList(
                            "Adding before multiplying",
                            "Ignoring brackets",
                            "Sign errors with negative numbers"));
                    a.getFollowUps().addAll(Arrays.asList(
                            "Try 2 + 3 * 4 without a calculator.",
                            "What changes if you add brackets: (2 + 3) * 4?"));
                }
                return a;
            } catch (RuntimeException ignored) {
                // fall through to guided explanation when parsing fails
            }
        }

        a.setDirectAnswer(directAnswerFor(subject, q));
        if (wantSteps) a.getSteps().addAll(approachFor(subject, q));
        a.setSimplified(simplifiedFor(subject));
        if (wantExtras) {
            a.getKeyConcepts().addAll(keyConceptsFor(subject));
            a.getCommonMistakes().addAll(mistakesFor(subject));
            a.getFollowUps().addAll(followUpsFor(subject));
        }
        return a;
    }

    private String formatNumber(double d) {
        if (d == Math.rint(d) && !Double.isInfinite(d)) {
            return String.valueOf((long) d);
        }
        return String.valueOf(Math.round(d * 10000.0) / 10000.0);
    }

    private String difficultyFor(String level) {
        if (level == null) return "Intermediate";
        String l = level.toLowerCase(Locale.ROOT);
        if (l.contains("middle")) return "Beginner";
        if (l.contains("high")) return "Intermediate";
        if (l.contains("university") || l.contains("college")) return "Advanced";
        return "Intermediate";
    }

    private String directAnswerFor(String subject, String q) {
        String topic = q.isEmpty() ? "this question" : "\"" + shorten(q) + "\"";
        switch (subject) {
            case SubjectClassifier.MATH:
                return "To work through " + topic + ", identify what is being asked, "
                        + "write down the known values, choose the right formula or method, "
                        + "then solve carefully and check the units of the result.";
            case SubjectClassifier.SCIENCE:
                return "For " + topic + ", state the scientific principle involved, list the "
                        + "quantities you know, apply the correct law or formula, and interpret "
                        + "what the result means in the real world.";
            case SubjectClassifier.PROGRAMMING:
                return "For " + topic + ", clarify the input and expected output, design the "
                        + "logic (data structure + algorithm) first, then translate it into code "
                        + "and test it with small examples and edge cases.";
            case SubjectClassifier.HISTORY:
                return "For " + topic + ", establish the time, place and people involved, then "
                        + "explain the causes, the key events and the consequences, supported by "
                        + "evidence.";
            case SubjectClassifier.LANGUAGES:
                return "For " + topic + ", identify the grammar rule or vocabulary in focus, apply "
                        + "it in a clear example sentence, and note how meaning changes with context.";
            default:
                return "Here is a structured way to approach " + topic + ": break it into smaller "
                        + "parts, deal with each part in turn, and summarise the conclusion at the end.";
        }
    }

    private List<String> approachFor(String subject, String q) {
        switch (subject) {
            case SubjectClassifier.MATH:
                return Arrays.asList(
                        "Restate the problem and list the known and unknown values.",
                        "Pick the formula or theorem that links them.",
                        "Substitute the values and simplify step by step.",
                        "Compute the final answer and check it is reasonable.");
            case SubjectClassifier.SCIENCE:
                return Arrays.asList(
                        "Identify the concept or law being tested.",
                        "Write down the given data with units.",
                        "Apply the relevant equation and rearrange for the unknown.",
                        "Substitute values and state the result with correct units.");
            case SubjectClassifier.PROGRAMMING:
                return Arrays.asList(
                        "Define the inputs, outputs and constraints.",
                        "Sketch the algorithm in plain steps (pseudocode).",
                        "Choose suitable data structures.",
                        "Implement, then test with normal and edge-case inputs.");
            case SubjectClassifier.HISTORY:
                return Arrays.asList(
                        "Place the topic in its time and location.",
                        "List the main causes.",
                        "Describe the key events in order.",
                        "Explain the short and long term consequences.");
            case SubjectClassifier.LANGUAGES:
                return Arrays.asList(
                        "Identify the grammar point or vocabulary involved.",
                        "State the rule simply.",
                        "Give a correct example sentence.",
                        "Show one common incorrect version and fix it.");
            default:
                return Arrays.asList(
                        "Break the question into smaller parts.",
                        "Answer each part with a short justification.",
                        "Combine the parts into a final conclusion.");
        }
    }

    private String simplifiedFor(String subject) {
        switch (subject) {
            case SubjectClassifier.MATH:
                return "Know what you have, know what you want, pick the method that connects them.";
            case SubjectClassifier.SCIENCE:
                return "Find the rule, plug in the numbers, keep the units.";
            case SubjectClassifier.PROGRAMMING:
                return "Plan the logic before you type, then test small examples.";
            case SubjectClassifier.HISTORY:
                return "Ask: who, when, why did it happen, and what changed afterwards.";
            case SubjectClassifier.LANGUAGES:
                return "Learn the rule with one clear example and one common mistake.";
            default:
                return "Split it up, solve the pieces, then bring them back together.";
        }
    }

    private List<String> keyConceptsFor(String subject) {
        switch (subject) {
            case SubjectClassifier.MATH:
                return Arrays.asList("Formulas and theorems", "Order of operations", "Checking reasonableness");
            case SubjectClassifier.SCIENCE:
                return Arrays.asList("Scientific laws", "Units and measurement", "Cause and effect");
            case SubjectClassifier.PROGRAMMING:
                return Arrays.asList("Algorithms", "Data structures", "Testing and debugging");
            case SubjectClassifier.HISTORY:
                return Arrays.asList("Chronology", "Cause and consequence", "Use of evidence");
            case SubjectClassifier.LANGUAGES:
                return Arrays.asList("Grammar rules", "Vocabulary in context", "Sentence structure");
            default:
                return Arrays.asList("Problem decomposition", "Reasoning", "Clear conclusions");
        }
    }

    private List<String> mistakesFor(String subject) {
        switch (subject) {
            case SubjectClassifier.MATH:
                return Arrays.asList("Skipping steps", "Wrong order of operations", "Forgetting units");
            case SubjectClassifier.SCIENCE:
                return Arrays.asList("Mixing up units", "Confusing similar laws", "Ignoring assumptions");
            case SubjectClassifier.PROGRAMMING:
                return Arrays.asList("Off-by-one errors", "Not handling edge cases", "Unclear variable names");
            case SubjectClassifier.HISTORY:
                return Arrays.asList("Confusing dates", "Listing events without causes", "No supporting evidence");
            case SubjectClassifier.LANGUAGES:
                return Arrays.asList("Wrong verb tense", "Literal translation", "Ignoring context");
            default:
                return Arrays.asList("Answering only part of the question", "No justification");
        }
    }

    private List<String> followUpsFor(String subject) {
        switch (subject) {
            case SubjectClassifier.MATH:
                return Arrays.asList("Try a similar problem with different numbers.",
                        "Explain the method to a friend in one sentence.");
            case SubjectClassifier.SCIENCE:
                return Arrays.asList("Give a real-life example of this principle.",
                        "What happens if one variable is doubled?");
            case SubjectClassifier.PROGRAMMING:
                return Arrays.asList("Rewrite the solution using a loop instead of recursion (or vice versa).",
                        "What is the time complexity of your solution?");
            case SubjectClassifier.HISTORY:
                return Arrays.asList("Compare this event with a similar one elsewhere.",
                        "How might things have changed if the outcome were different?");
            case SubjectClassifier.LANGUAGES:
                return Arrays.asList("Write two more sentences using the same rule.",
                        "Find a synonym and use it in context.");
            default:
                return Arrays.asList("Summarise the answer in one line.",
                        "List one question this raises for you.");
        }
    }

    private String shorten(String q) {
        String s = q.replace("\n", " ").trim();
        return s.length() > 80 ? s.substring(0, 77) + "..." : s;
    }

    // ---------------------------------------------------------------------
    //  Practice / quiz generation
    // ---------------------------------------------------------------------

    @Override
    public List<QuizQuestion> generateQuiz(String subject, int count) {
        if (count <= 0) count = 5;
        String subj = SubjectClassifier.normalize(subject);

        List<QuizQuestion> bank = new ArrayList<>(bankFor(subj));

        // Maths gets procedurally generated arithmetic questions for endless variety.
        if (subj.equals(SubjectClassifier.MATH)) {
            Random rnd = new Random();
            while (bank.size() < count + 3) bank.add(generateArithmetic(rnd));
        }

        Collections.shuffle(bank);
        if (bank.size() > count) {
            return new ArrayList<>(bank.subList(0, count));
        }
        return bank;
    }

    @Override
    public List<QuizQuestion> generateQuiz(QuizGenerationConfig config) {
        List<QuizQuestion> generated =
                generateQuiz(config.getSubject(), config.getCount());
        List<QuizQuestion> adapted = new ArrayList<>();
        for (QuizQuestion question : generated) {
            adapted.add(new QuizQuestion(
                    question.getPrompt(), new ArrayList<>(question.getOptions()),
                    question.getCorrectIndex(), question.getExplanation(),
                    config.getSubject(), config.getDifficulty(), question.getType()));
        }
        return adapted;
    }

    private QuizQuestion generateArithmetic(Random rnd) {
        int a = 2 + rnd.nextInt(20);
        int b = 2 + rnd.nextInt(20);
        int op = rnd.nextInt(3);
        int answer;
        String sign;
        if (op == 0) { answer = a + b; sign = "+"; }
        else if (op == 1) { answer = a * b; sign = "x"; }
        else { answer = a - b; sign = "-"; }
        List<String> options = new ArrayList<>();
        options.add(String.valueOf(answer));
        while (options.size() < 4) {
            int distractor = answer + (rnd.nextInt(9) - 4);
            String d = String.valueOf(distractor);
            if (!options.contains(d)) options.add(d);
        }
        Collections.shuffle(options);
        int correct = options.indexOf(String.valueOf(answer));
        return new QuizQuestion("What is " + a + " " + sign + " " + b + "?",
                options, correct, "Compute " + a + " " + sign + " " + b + " = " + answer + ".",
                SubjectClassifier.MATH);
    }

    private List<QuizQuestion> bankFor(String subject) {
        List<QuizQuestion> list = new ArrayList<>();
        switch (subject) {
            case SubjectClassifier.MATH:
                list.add(new QuizQuestion("Solve for x: 2x + 6 = 14",
                        Arrays.asList("x = 2", "x = 4", "x = 5", "x = 8"), 1,
                        "2x = 14 - 6 = 8, so x = 4.", subject));
                list.add(new QuizQuestion("What is 25% of 200?",
                        Arrays.asList("25", "40", "50", "75"), 2,
                        "25% = 0.25, and 0.25 x 200 = 50.", subject));
                list.add(trueFalse("A triangle's interior angles add up to 180 degrees.",
                        true, "In Euclidean geometry, the three interior angles total 180 degrees.",
                        subject, "Beginner"));
                break;
            case SubjectClassifier.SCIENCE:
                list.add(new QuizQuestion("What is the chemical symbol for water?",
                        Arrays.asList("O2", "H2O", "CO2", "NaCl"), 1,
                        "Water is two hydrogen atoms and one oxygen atom: H2O.", subject));
                list.add(new QuizQuestion("Which force pulls objects toward Earth?",
                        Arrays.asList("Friction", "Magnetism", "Gravity", "Tension"), 2,
                        "Gravity is the attractive force toward Earth's centre.", subject));
                list.add(new QuizQuestion("The powerhouse of the cell is the...",
                        Arrays.asList("Nucleus", "Ribosome", "Mitochondria", "Membrane"), 2,
                        "Mitochondria produce most of the cell's energy (ATP).", subject));
                list.add(trueFalse("Sound can travel through a vacuum.",
                        false, "Sound needs particles in a medium, so it cannot travel through a vacuum.",
                        subject, "Intermediate"));
                list.add(new QuizQuestion("Which particle has a negative electric charge?",
                        Arrays.asList("Proton", "Neutron", "Electron", "Photon"), 2,
                        "Electrons carry negative electric charge.", subject));
                break;
            case SubjectClassifier.PROGRAMMING:
                list.add(new QuizQuestion("Which data structure works First-In-First-Out (FIFO)?",
                        Arrays.asList("Stack", "Queue", "Tree", "Graph"), 1,
                        "A queue removes items in the order they were added (FIFO).", subject));
                list.add(new QuizQuestion("What does a 'for' loop do?",
                        Arrays.asList("Declares a class", "Repeats a block of code",
                                "Defines a variable", "Handles an error"), 1,
                        "A for loop repeats a block a controlled number of times.", subject));
                list.add(new QuizQuestion("In Java, which keyword creates a new object?",
                        Arrays.asList("class", "void", "new", "static"), 2,
                        "The 'new' keyword allocates and instantiates an object.", subject));
                list.add(trueFalse("An array index normally starts at zero in Java.",
                        true, "Java arrays use zero-based indexing.", subject, "Beginner"));
                list.add(new QuizQuestion("Which SQL command reads rows from a table?",
                        Arrays.asList("SELECT", "UPDATE", "DELETE", "DROP"), 0,
                        "SELECT retrieves rows without changing them.", subject));
                break;
            case SubjectClassifier.HISTORY:
                list.add(new QuizQuestion("In which year did World War II end?",
                        Arrays.asList("1918", "1939", "1945", "1963"), 2,
                        "World War II ended in 1945.", subject));
                list.add(new QuizQuestion("Ancient pyramids of Giza were built in...",
                        Arrays.asList("Greece", "Egypt", "Rome", "China"), 1,
                        "The Giza pyramids are in Egypt.", subject));
                list.add(trueFalse("The Industrial Revolution began in Britain.",
                        true, "Britain industrialised first during the late 18th century.",
                        subject, "Intermediate"));
                list.add(new QuizQuestion("The Renaissance began in which country?",
                        Arrays.asList("Italy", "Spain", "Germany", "Norway"), 0,
                        "The Renaissance began in Italian city-states before spreading.", subject));
                list.add(new QuizQuestion("Which event began in 1789?",
                        Arrays.asList("French Revolution", "American Civil War",
                                "World War I", "Russian Revolution"), 0,
                        "The French Revolution began in 1789.", subject));
                break;
            case SubjectClassifier.LANGUAGES:
                list.add(new QuizQuestion("Choose the correct sentence:",
                        Arrays.asList("She go to school.", "She goes to school.",
                                "She going school.", "She gone school."), 1,
                        "Third-person singular present adds -s: 'She goes'.", subject));
                list.add(new QuizQuestion("What is a synonym of 'happy'?",
                        Arrays.asList("Sad", "Joyful", "Angry", "Tired"), 1,
                        "'Joyful' means the same as 'happy'.", subject));
                list.add(trueFalse("An adjective describes a noun.",
                        true, "Adjectives add information about nouns.", subject, "Beginner"));
                list.add(new QuizQuestion("Choose the past tense of 'go':",
                        Arrays.asList("Goed", "Gone", "Went", "Going"), 2,
                        "'Went' is the simple past form of 'go'.", subject));
                list.add(new QuizQuestion("Which word is an adverb?",
                        Arrays.asList("Quick", "Quickly", "Quicker", "Quickness"), 1,
                        "'Quickly' describes how an action happens.", subject));
                break;
            default:
                list.add(new QuizQuestion("Good problem solving usually starts by...",
                        Arrays.asList("Guessing the answer", "Breaking the problem into parts",
                                "Copying a friend", "Skipping it"), 1,
                        "Decomposing a problem into smaller parts makes it manageable.", subject));
                list.add(new QuizQuestion("Reviewing past questions helps you...",
                        Arrays.asList("Forget faster", "Reinforce learning",
                                "Waste time", "Lower your score"), 1,
                        "Spaced review reinforces memory and understanding.", subject));
                list.add(trueFalse("Checking evidence improves the reliability of an answer.",
                        true, "Evidence helps verify whether a claim is well supported.",
                        subject, "Beginner"));
                list.add(new QuizQuestion("Which action best supports focused study?",
                        Arrays.asList("Set a clear goal", "Keep every notification on",
                                "Skip all breaks", "Multitask constantly"), 0,
                        "A clear goal directs attention and makes progress measurable.", subject));
                list.add(new QuizQuestion("A useful summary should primarily...",
                        Arrays.asList("Repeat every word", "Capture the main ideas",
                                "Add unrelated details", "Avoid conclusions"), 1,
                        "A summary condenses the most important ideas.", subject));
                break;
        }
        return list;
    }

    private QuizQuestion trueFalse(String prompt, boolean answer,
                                   String explanation, String subject,
                                   String difficulty) {
        return new QuizQuestion(prompt, Arrays.asList("True", "False"),
                answer ? 0 : 1, explanation, subject, difficulty,
                QuizQuestion.Type.TRUE_FALSE);
    }
}
