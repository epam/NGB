/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.epam.catgenome.util;

import com.epam.catgenome.constant.MessagesConstants;
import htsjdk.samtools.util.RuntimeIOException;
import htsjdk.samtools.util.TempStreamFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;

import static com.epam.catgenome.component.MessageHelper.getMessage;

/**
 * Simple disk based collection based on ArrayList buffer. Provides simple interface adding,
 * iterating and clearing items stored.
 * @param <T> - class of items to be stored
 */
public class DiskBasedList<T> implements Iterable<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiskBasedList.class);

    private static final int DEFAULT_IN_MEMORY_COUNT = 100000;
    private static final int BUFFER_SIZE = 256 * 1024;

    private final int maxInMemoryItemsCount;
    private ArrayList<T> buffer;
    private ArrayList<File> batchFiles;

    private TempStreamFactory tempStreamFactory = new TempStreamFactory();

    public DiskBasedList(int maxInMemoryItemsCount) {
        this.maxInMemoryItemsCount = maxInMemoryItemsCount;
        this.buffer = new ArrayList<>();
        this.batchFiles = new ArrayList<>();
    }

    public DiskBasedList() {
        this(DEFAULT_IN_MEMORY_COUNT);
    }

    @Override
    public Iterator<T> iterator() {
        if (batchFiles.isEmpty()) {
            return buffer.iterator();
        } else {
            return new DiskBasedListIterator();
        }
    }

    public void add(T item) {
        buffer.add(item);

        if (buffer.size() >= maxInMemoryItemsCount) {
            spillBuffer();
        }
    }

    public void clear() {
        buffer.clear();
        batchFiles.forEach(File::delete);
        batchFiles.clear();
    }

    /**
     * Adapts current instance to the {@link List} interface
     * @return adapted to the {@link List} interface {@link DiskBasedList} instance
     */
    public List<T> adaptToList() {
        return new DiskBasedListToListAdapter<>(this);
    }

    private void spillBuffer() {
        LOGGER.debug("spilling...");

        try {
            File batchFile = File.createTempFile(this.getClass().getName(), ".batch");
            batchFile.deleteOnExit();

            try (ObjectOutputStream oos = new ObjectOutputStream(
                    tempStreamFactory.wrapTempOutputStream(new FileOutputStream(batchFile), BUFFER_SIZE))) {
                for (T item : buffer) {
                    oos.writeObject(item);
                }
                oos.flush();
            }

            batchFiles.add(batchFile);
            LOGGER.debug("spilled {} items to the file ({} Kb)", buffer.size(), batchFile.length() / 1024);
            buffer.clear();

        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    private class DiskBasedListIterator implements Iterator<T> {

        private final Iterator<File> batchFilesIterator = batchFiles.iterator();
        private final Iterator<T> restBufferIterator = buffer.iterator();

        private ObjectInputStream currentBatchFileInputStream;
        private int objectsAlreadyReadFromFile;

        @Override
        public boolean hasNext() {
            return hasMoreInCurrentFile()
                    || batchFilesIterator.hasNext()
                    || restBufferIterator.hasNext();

        }

        @Override
        public T next() {
            try {
                if (!hasMoreInCurrentFile()) {
                    if (batchFilesIterator.hasNext()) {
                        currentBatchFileInputStream = new ObjectInputStream(
                                tempStreamFactory.wrapTempInputStream(
                                        new FileInputStream(batchFilesIterator.next()), BUFFER_SIZE));
                        objectsAlreadyReadFromFile = 0;
                        return next();
                    } else {
                        return restBufferIterator.next();
                    }
                } else {
                    T deserializedItem = (T) currentBatchFileInputStream.readObject();
                    objectsAlreadyReadFromFile++;
                    return deserializedItem;
                }
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeIOException(e);
            }
        }

        private boolean hasMoreInCurrentFile() {
            return currentBatchFileInputStream != null && objectsAlreadyReadFromFile < maxInMemoryItemsCount;
        }
    }

    private static class DiskBasedListToListAdapter<T> implements List<T> {

        private DiskBasedList<T> diskBasedList;

        DiskBasedListToListAdapter(DiskBasedList<T> diskBasedList) {
            this.diskBasedList = diskBasedList;
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_OPERATION));
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_OPERATION));
        }

        @Override
        public boolean contains(Object o) {
            throw new UnsupportedOperationException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_OPERATION));
        }

        @NotNull
        @Override
        public Iterator<T> iterator() {
            return diskBasedList.iterator();
        }

        @NotNull
        @Override
        public Object[] toArray() {
            throw new UnsupportedOperationException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_OPERATION));
        }

        @NotNull
        @Override
        public <T1> T1[] toArray(@NotNull T1[] a) {
            throw new UnsupportedOperationException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_OPERATION));
        }

        @Override
        public boolean add(T t) {
            diskBasedList.add(t);
            return true;
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_OPERATION));
        }

        @Override
        public boolean containsAll(@NotNull Collection<?> c) {
            throw new UnsupportedOperationException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_OPERATION));
        }

        @Override
        public boolean addAll(@NotNull Collection<? extends T> c) {
            for (T t : c) {
                add(t);
            }
            return true;
        }

        @Override
        public boolean addAll(int index, @NotNull Collection<? extends T> c) {
            throw new UnsupportedOperationException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_OPERATION));
        }

        @Override
        public boolean removeAll(@NotNull Collection<?> c) {
            throw new UnsupportedOperationException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_OPERATION));
        }

        @Override
        public boolean retainAll(@NotNull Collection<?> c) {
            throw new UnsupportedOperationException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_OPERATION));
        }

        @Override
        public void clear() {
            diskBasedList.clear();
        }

        @Override
        public T get(int index) {
            throw new UnsupportedOperationException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_OPERATION));
        }

        @Override
        public T set(int index, T element) {
            throw new UnsupportedOperationException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_OPERATION));
        }

        @Override
        public void add(int index, T element) {
            throw new UnsupportedOperationException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_OPERATION));
        }

        @Override
        public T remove(int index) {
            throw new UnsupportedOperationException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_OPERATION));
        }

        @Override
        public int indexOf(Object o) {
            throw new UnsupportedOperationException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_OPERATION));
        }

        @Override
        public int lastIndexOf(Object o) {
            throw new UnsupportedOperationException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_OPERATION));
        }

        @NotNull
        @Override
        public ListIterator<T> listIterator() {
            throw new UnsupportedOperationException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_OPERATION));
        }

        @NotNull
        @Override
        public ListIterator<T> listIterator(int index) {
            throw new UnsupportedOperationException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_OPERATION));
        }

        @NotNull
        @Override
        public List<T> subList(int fromIndex, int toIndex) {
            throw new UnsupportedOperationException(getMessage(MessagesConstants.ERROR_UNSUPPORTED_OPERATION));
        }

    }

}

