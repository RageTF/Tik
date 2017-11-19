package shenon;

import java.io.*;
import java.util.*;

public class AlgorithmShennonaCode {

    private File mInputFile;
    private File mOutputFile;
    private Map<Byte, Integer> mByteCountMap;
    private List<SymbolProbability> mSymbolProbabilityList;
    private Map<Byte, String> mCodeTable;
    private int mCountSymbol = 0;
    private int mByCountByte = 1;

    public AlgorithmShennonaCode(String inputFile, String outputFile) {
        mInputFile = new File(inputFile);
        mOutputFile = new File(outputFile);
        mByteCountMap = new HashMap<>();
        mSymbolProbabilityList = new ArrayList<>();
        mCodeTable = new HashMap<>();
        mCountSymbol = 0;
        mByCountByte = 1;
    }

    public AlgorithmShennonaCode(String inputFile, String outputFile, int byCount) {
        this(inputFile, outputFile);
        mByCountByte = byCount;
    }


    public static void main(String[] args) {
        AlgorithmShennonaCode algorithmShennonaCode = new AlgorithmShennonaCode("E:\\Java\\entropy.txt", "E:\\Java\\fileOutput.khan");
        algorithmShennonaCode.code();
    }

    public void code() {
        try {
            readFile();
            computeProbability();
            if (mSymbolProbabilityList.size() > 1) {
                searchTree(0, mSymbolProbabilityList.size(), "");
            } else {
                return;
            }

            FileOutputStream fileOutputStream = new FileOutputStream(mOutputFile);
            FileInputStream fileInputStream = new FileInputStream(mInputFile);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);
            DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);
            int countSymbols=mSymbolProbabilityList.size();
            dataOutputStream.writeInt(countSymbols);
            mCodeTable.forEach((aByte, s) -> {
                try {
                    dataOutputStream.writeByte(aByte);
                    dataOutputStream.writeByte(s.length());
                    StringBuilder stringBuilder=new StringBuilder(s);
                    while (stringBuilder.length()>=8){
                        String b=stringBuilder.substring(0,8);
                        stringBuilder.delete(0,8);
                        dataOutputStream.writeByte(Integer.parseInt(b,2));
                    }
                    if(stringBuilder.length()>0){
                        dataOutputStream.writeByte(Integer.parseInt(stringBuilder.toString(),2));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            int b;
            StringBuilder writeBytes = new StringBuilder();
            while ((b = dataInputStream.read()) != -1) {
                writeBytes.append(mCodeTable.get((byte)b));
                while (writeBytes.length() >= 8) {
                    dataOutputStream.write((byte)Integer.parseInt(writeBytes.substring(0, 8), 2));
                    writeBytes.delete(0, 8);
                }
            }
            int emptyPostfix=8-writeBytes.length();
            for (int i = 0; i <emptyPostfix; i++) {
                writeBytes.append("0");
            }
            dataOutputStream.write((byte)Integer.parseInt(writeBytes.substring(0,8),2));
            dataOutputStream.write((byte)emptyPostfix);
            dataOutputStream.flush();
            dataInputStream.close();
            dataOutputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readFile() throws IOException {
        FileInputStream fileInputStream = new FileInputStream(mInputFile);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
        int i;
        while ((i = bufferedInputStream.read()) != -1) {
            mCountSymbol++;
            byte b = (byte) i;
            if (mByteCountMap.containsKey(b)) {
                int count = mByteCountMap.get(b);
                mByteCountMap.replace(b, count + 1);
            } else {
                mByteCountMap.put(b, 1);
            }
        }
        fileInputStream.close();
    }

    private void computeProbability() {
        mByteCountMap.forEach((symbol, count) -> mSymbolProbabilityList.add(new SymbolProbability(symbol, (double) count / (double) mCountSymbol)));
        Collections.sort(mSymbolProbabilityList);
    }

    private void searchTree(int start, int end, String code) {

        double divQ = 0;

        if (Math.abs(end - start) == 1) {
            SymbolProbability symbolProbability = mSymbolProbabilityList.get(start);
            mCodeTable.put(symbolProbability.mSymbol, code);
            return;
        }

        for (int i = start; i < end; i++) {
            divQ += mSymbolProbabilityList.get(i).mProbability;
        }
        divQ /= 2;

        int i = start;
        double div = 0;

        while (i < end && div < divQ) {
            div += mSymbolProbabilityList.get(i).mProbability;
            i++;
        }
        searchTree(start, i, code + "0");
        searchTree(i, end, code + "1");
    }

    private static class SymbolProbability implements Comparable<SymbolProbability> {
        private byte mSymbol;
        private double mProbability;

        public SymbolProbability(byte symbol, double probability) {
            mSymbol = symbol;
            mProbability = probability;
        }

        public byte getSymbol() {
            return mSymbol;
        }

        public void setSymbol(byte symbol) {
            mSymbol = symbol;
        }

        public double getProbability() {
            return mProbability;
        }

        public void setProbability(double probability) {
            mProbability = probability;
        }

        @Override
        public int compareTo(SymbolProbability o) {
            if (mProbability < o.mProbability) {
                return 1;
            } else if (mProbability > o.mProbability) {
                return -1;
            }
            return 0;
        }
    }

}
