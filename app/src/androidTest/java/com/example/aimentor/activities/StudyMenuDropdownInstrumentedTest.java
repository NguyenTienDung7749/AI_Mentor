package com.example.aimentor.activities;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.aimentor.R;
import com.example.aimentor.util.SessionManager;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class StudyMenuDropdownInstrumentedTest {

    @Test
    public void selectedDropdowns_stillExposeCompleteCatalogs() {
        Context context = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        SessionManager session = new SessionManager(context);
        long previousUserId = session.getCurrentUserId();
        Activity activity = null;
        try {
            session.setCurrentUserId(Long.MAX_VALUE);
            Intent intent = new Intent(context, MenuActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity = InstrumentationRegistry.getInstrumentation()
                    .startActivitySync(intent);

            onView(withId(R.id.spSubject)).perform(scrollTo(), click());
            assertPopupContains("Mathematics");
            pressBack();

            onView(withId(R.id.Category_menu)).perform(click());
            onView(withId(R.id.actSubjectFilter)).perform(click());
            assertPopupContains("General");
            pressBack();

            onView(withId(R.id.Quiz_menu)).perform(click());
            onView(withId(R.id.actQuizSubject)).perform(click());
            assertPopupContains("History");
            pressBack();
            onView(withId(R.id.actQuizDifficulty)).perform(click());
            assertPopupContains("Advanced");
        } finally {
            if (activity != null) activity.finish();
            if (previousUserId > 0L) {
                session.setCurrentUserId(previousUserId);
            } else {
                session.logout();
            }
        }
    }

    private void assertPopupContains(String item) {
        onData(allOf(instanceOf(String.class), is(item)))
                .inRoot(isPlatformPopup())
                .check(matches(isDisplayed()));
    }
}
