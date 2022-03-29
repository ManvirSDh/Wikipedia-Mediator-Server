package cpen221.mp3;

import cpen221.mp3.fsftbuffer.*;
import org.junit.Test;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class Tests {

    /*
            You can add your tests here.
            Remember to import the packages that you need, such
            as cpen221.mp3.fsftbuffer.
         */


    // TASK 1 TESTS

    @Test
    public void test_defaultConstructor() {
        FSFTBuffer buffer = new FSFTBuffer();
        Bufferable t = new BufferableString("Default constructor test");
        assertTrue(buffer.put(t));
        try {
            Thread.sleep(2500);
        } catch (InterruptedException ie) {
            fail("threw wrong exception");
        }

        boolean touched = buffer.touch(t.id());
        assertTrue(touched);
    }


    @Test
    public void test_put1() {
        FSFTBuffer buffer = new FSFTBuffer(1, 1);
        Bufferable t = new BufferableString("Test");
        assertTrue(buffer.put(t));
        try {
            Bufferable obj = buffer.get(t.id());
            assertEquals(obj, t);
        } catch (Exception e) {
            System.out.println("Test failed");
        }
    }

    @Test
    public void test_put2() {
        FSFTBuffer buffer = new FSFTBuffer(1, 10000);
        Bufferable t1 = new BufferableString("string");
        Bufferable t2 = new BufferableString("test");
        assertTrue(buffer.put(t1));
        assertTrue(buffer.put(t2));
        try {
            Bufferable obj = buffer.get(t2.id());
            assertEquals(obj, t2);
        } catch (Exception e) {
            System.out.println("Test failed");
        }
    }

    @Test(expected = ObjectNotFoundException.class)
    public void test_put3() throws ObjectNotFoundException {
        FSFTBuffer buffer = new FSFTBuffer(2, 111);
        Bufferable t1 = new BufferableString("cpen221");
        Bufferable t2 = new BufferableString("cpen-221");
        Bufferable t3 = new BufferableString("computer engineering");

        try {
            assertTrue(buffer.put(t1));
            Thread.sleep(100);
            assertTrue(buffer.put(t2));
            Thread.sleep(500);
            assertTrue(buffer.put(t3));
            Bufferable obj2 = buffer.get(t2.id());
            assertEquals(obj2, t2);
            Bufferable obj3 = buffer.get(t3.id());
            assertEquals(obj3, t3);
        } catch (Exception e) {
            fail("Test failed.");
        }

        buffer.get(t1.id());
    }


    @Test
    public void test_put4() {
        FSFTBuffer buffer = new FSFTBuffer(1, 2);
        Bufferable t = new BufferableString("2222");
        assertTrue(buffer.put(t));
        try {
            Thread.sleep(1500);
        } catch (Exception e) {
            fail("Exception not expected");
        }

        assertFalse(buffer.put(t));

        try {
            Thread.sleep(600);
        } catch (Exception e) {
            fail("Test failed.");
        }

        assertTrue(buffer.put(t));
    }

    @Test
    public void test_get1() {
        FSFTBuffer buffer = new FSFTBuffer(1, 1);
        Bufferable t = new BufferableString("Software Construction");
        assertTrue(buffer.put(t));
        try {
            Bufferable obj = buffer.get(t.id());
            assertEquals(obj, t);
        } catch (ObjectNotFoundException e) {
            fail("Test failed.");
        }
    }

    @Test(expected = ObjectNotFoundException.class)
    public void test_get2() throws ObjectNotFoundException {
        FSFTBuffer buffer = new FSFTBuffer(1, 1);
        Bufferable t = new BufferableString("123456789");
        assertTrue(buffer.put(t));

        try {
            Thread.sleep(1010);
        } catch (InterruptedException ie) {
            fail("Threw wrong exception");
        }

        buffer.get(t.id());
    }

    @Test
    public void test_get3() {
        FSFTBuffer buffer = new FSFTBuffer(4, 1);
        Bufferable t1 = new BufferableString("a");
        Bufferable t2 = new BufferableString("b");
        Bufferable t3 = new BufferableString("c");
        Bufferable t4 = new BufferableString("d");
        Bufferable t5 = new BufferableString("e");

        try {
            assertTrue(buffer.put(t1));
            Thread.sleep(1010);
            assertTrue(buffer.put(t2));
            Thread.sleep(1010);
            assertTrue(buffer.put(t3));
            Thread.sleep(1010);
            assertTrue(buffer.put(t4));
        } catch (InterruptedException ie) {
            fail("Wrong exception");
        }

        buffer.touch(t1.id());

        assertTrue(buffer.put(t5));

        try {
            buffer.get("b");
            fail("Test failed");
        } catch (ObjectNotFoundException e) {
            assertFalse(buffer.touch("b"));
        }
    }

    @Test
    public void test_touch1() {
        FSFTBuffer buffer = new FSFTBuffer(1, 1);
        Bufferable t = new BufferableString("Software Construction");
        assertTrue(buffer.put(t));
        try {
            Thread.sleep(800);
            boolean touched = buffer.touch(t.id());
            Bufferable obj = buffer.get(t.id());
            assertEquals(obj, t);
            assertTrue(touched);
        } catch (Exception e) {
            fail("Test failed.");
        }
    }

    @Test
    public void test_touch2() {
        FSFTBuffer buffer = new FSFTBuffer(1, 1);
        Bufferable t = new BufferableString("123456789");
        assertTrue(buffer.put(t));
        try {
            Thread.sleep(1010);
        } catch (InterruptedException ie) {
            fail("Wrong exception");
        }

        boolean touched = buffer.touch(t.id());
        assertFalse(touched);
    }

    @Test
    public void test_update1() {
        FSFTBuffer buffer = new FSFTBuffer(1, 1);
        Bufferable t1 = new BufferableString("abcde");
        Bufferable t2 = new BufferableString("abcde");
        assertTrue(buffer.put(t1));
        try {
            Thread.sleep(600);
        } catch (InterruptedException ie) {
            fail("Wrong exception");
        }

        assertTrue(buffer.update(t2));
    }

    @Test
    public void test_update2() {
        FSFTBuffer buffer = new FSFTBuffer(1, 1);
        Bufferable t1 = new BufferableString("1");
        Bufferable t2 = new BufferableString("abcde");
        Bufferable t3 = new BufferableString("abcde");
        assertTrue(buffer.put(t1));
        try {
            Thread.sleep(1010);
            assertTrue(buffer.put(t2));
        } catch (InterruptedException ie) {
            fail("threw wrong exception");
        }

        assertFalse(buffer.update(t1));
        assertTrue(buffer.update(t3));
    }

    // TASK 2 TESTS

    @Test
    public void test_safeConcurrency1() throws InterruptedException {
        FSFTBuffer buffer = new FSFTBuffer(5, 1);
        Test_Task2 testBuffer = new Test_Task2(buffer);

        Thread testThread1 = new Thread(testBuffer);
        Thread testThread2 = new Thread(testBuffer);
        Thread testThread3 = new Thread(testBuffer);

        testThread1.start();
        testThread2.start();
        testThread3.start();
        testThread1.join();
        testThread2.join();
        testThread3.join();
    }

    @Test
    public void test_safeConcurrency2() throws InterruptedException {
        for (int i = 0; i < 500; i++) {
            test_safeConcurrency1();
        }
    }

    @Test
    public void test_safeConcurrency3() throws InterruptedException {
        FSFTBuffer buffer = new FSFTBuffer(3, 1);
        Test_Task2 testBuffer = new Test_Task2(buffer);

        Thread testThread1 = new Thread(testBuffer);
        Thread testThread2 = new Thread(testBuffer);
        Thread testThread3 = new Thread(testBuffer);

        testThread1.start();
        testThread2.start();
        testThread3.start();
        testThread1.join();
        testThread2.join();
        testThread3.join();
    }

    @Test
    public void test_Concurrency4() throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            test_safeConcurrency3();
        }
    }

    private class Test_Task2 implements Runnable
    {
        FSFTBuffer<BufferableString> buffer;

        public Test_Task2(FSFTBuffer<BufferableString> b) {
            buffer = b;
        }

        @Override
        public void run() {
            BufferableString t1 = new BufferableString("a");
            BufferableString t2 = new BufferableString("b");
            BufferableString t3 = new BufferableString("c");
            BufferableString t4 = new BufferableString("d");
            BufferableString t5 = new BufferableString("e");
            BufferableString t6 = new BufferableString("c");

            try {
                buffer.put(t1);
                Thread.sleep(1);
                buffer.put(t2);
                Thread.sleep(1);
                buffer.put(t3);
                Thread.sleep(1);
                buffer.put(t4);
                Thread.sleep(1);
                buffer.put(t5);

                buffer.touch("c");
                buffer.update(t6);
            } catch (InterruptedException ie) {
                fail("Exception not expected");
            }

            try {
                buffer.get("a");
                buffer.get("b");
                buffer.get("c");
                buffer.touch("d");
                buffer.get("d");
                buffer.get("e");
            } catch (ObjectNotFoundException e) {
                buffer.update(t2);
            }
        }
    }
}