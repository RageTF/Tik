package entropy;

import io.reactivex.Observable;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Entropy {

    private String mPath;
    private File mFile;
    private byte[] mText;
    private int mCountSymbols;
    private int mByCountSymbol;

    public Entropy(String path, int byCountSymbol) throws IOException {
        mPath = path;
        mByCountSymbol = byCountSymbol;
        mFile = new File(path);
        mText = readFile();
    }

    private byte[] readFile() throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(mFile)) {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            int symbol = -1;
            int pos = 0;
            byte[] content = new byte[bufferedInputStream.available()];
            while ((symbol = bufferedInputStream.read()) != -1) {
                content[pos++] = (byte) symbol;
            }
            return content;
        }
    }

    private Map<Sequence, Integer> symbolsMap() {
        Map<Sequence, Integer> sequenceIntegerMap = new HashMap<>();
        for (int i = 0; i < mText.length; i++) {
            int edge = mByCountSymbol;
            while (edge > 0) {
                if (i + edge - 1 < mText.length) {
                    byte[] seq = new byte[edge];
                    for (int k = 0; k < edge; k++) {
                        seq[k] = mText[i + k];
                    }
                    Sequence sequence = new Sequence(seq);
                    putComputeSequence(sequenceIntegerMap, sequence);
                }
                edge--;
            }

        }
        return sequenceIntegerMap;
    }

    private void putComputeSequence(Map<Sequence, Integer> sequenceIntegerMap, Sequence sequence) {
        if (sequenceIntegerMap.containsKey(sequence)) {
            int count = sequenceIntegerMap.get(sequence);
            sequenceIntegerMap.put(sequence, count + 1);
        } else {
            sequenceIntegerMap.put(sequence, 1);
        }
    }

    private int countSymbols(Map<Sequence, Integer> sequenceIntegerMap, int length) {
        int count = 0;
        for (Map.Entry<Sequence, Integer> entry : sequenceIntegerMap.entrySet()) {
            Sequence sequence = entry.getKey();
            if (sequence.getLength() == length) {
                count += entry.getValue();
            }
        }
        return count;
    }

    private boolean equalByStart(byte[] array, byte[] array1, int from, int to) {
        if (array.length >= to && array1.length >= to) {
            for (int i = from; i < to; i++) {
                if (array[i] != array1[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private double computeEntropy() {
        Map<Sequence, Integer> sequenceIntegerMap = symbolsMap();
        double entropy = 0;
        double countAllSymbols = countSymbols(sequenceIntegerMap, 1);
        double countCurrentSequence = countSymbols(sequenceIntegerMap, mByCountSymbol);
        double countPreviousSequence = countSymbols(sequenceIntegerMap, mByCountSymbol - 1);
        for (Map.Entry<Sequence, Integer> entry : sequenceIntegerMap.entrySet()) {
            Sequence sequence = entry.getKey();
            double countSeq = entry.getValue();
            if (mByCountSymbol == 1) {
                if (sequence.getLength() == 1) {
                    double q = countSeq / countAllSymbols;
                    entropy = entropy - q * log2(q);
                }
            } else {
                if (sequence.getLength() == mByCountSymbol - 1) {
                    double localEnt = 0;
                    for (Sequence sequence1 : sequenceIntegerMap.keySet()) {
                        if (sequence1.getLength() == mByCountSymbol
                                && equalByStart(sequence1.mBytes, sequence.mBytes, 0, mByCountSymbol - 1)) {
                            double symSeqNum = sequenceIntegerMap.get(sequence1);
                            double q = symSeqNum * countPreviousSequence / countSeq / countCurrentSequence;
                            localEnt += q * log2(q);
                        }
                    }
                    entropy -= countSeq / countPreviousSequence * localEnt;
                }
            }
        }
        return entropy;
    }

    private double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    private static class Sequence {
        byte[] mBytes;

        public Sequence(byte[] bytes) {
            mBytes = bytes;
        }

        int getLength() {
            return mBytes.length;
        }

        @Override
        public int hashCode() {
            int i = 0;
            for (byte b : mBytes)
                i += b;
            return i;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Sequence) {
                Sequence sequence = (Sequence) obj;
                if (sequence.mBytes.length != mBytes.length)
                    return false;
                for (int i = 0; i < mBytes.length; i++) {
                    if (mBytes[i] != sequence.mBytes[i]) {
                        return false;
                    }
                }
            } else {
                return false;
            }
            return true;
        }

    }

    public static void main(String[] args) {
        try {
            Entropy entropy = new Entropy("E:\\Java\\entropy.txt", 1);
            System.out.println(entropy.computeEntropy());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
