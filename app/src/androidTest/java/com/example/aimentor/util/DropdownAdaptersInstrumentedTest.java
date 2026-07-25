package com.example.aimentor.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.widget.ArrayAdapter;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class DropdownAdaptersInstrumentedTest {

    @Test
    public void selectedValue_doesNotFilterRemainingCatalog() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        String[] catalog = {
                "Auto", "Mathematics", "Science", "Programming"
        };
        CountDownLatch filtered = new CountDownLatch(1);
        AtomicInteger visibleCount = new AtomicInteger();
        AtomicReference<ArrayAdapter<String>> adapterReference =
                new AtomicReference<>();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ArrayAdapter<String> adapter =
                    DropdownAdapters.create(context, catalog);
            adapterReference.set(adapter);
            adapter.getFilter().filter("Auto", count -> {
                visibleCount.set(count);
                filtered.countDown();
            });
        });

        assertTrue(filtered.await(2, TimeUnit.SECONDS));
        assertEquals(catalog.length, visibleCount.get());
        assertEquals(catalog.length, adapterReference.get().getCount());
    }
}
