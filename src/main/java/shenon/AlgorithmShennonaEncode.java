package shenon;

import io.reactivex.Observable;

import java.io.*;
import java.util.*;

public class AlgorithmShennonaEncode {

    private File mInputFile;
    private File mOutputFile;
    private Map<Byte, String> mCodeTable;

    private static int[] sBitMasks = new int[]{
            Integer.parseInt("10000000", 2),
            Integer.parseInt("01000000", 2),
            Integer.parseInt("00100000", 2),
            Integer.parseInt("00010000", 2),
            Integer.parseInt("00001000", 2),
            Integer.parseInt("00000100", 2),
            Integer.parseInt("00000010", 2),
            Integer.parseInt("00000001", 2),
    };

    public AlgorithmShennonaEncode(String inputFile, String outputFile) {
        mInputFile = new File(inputFile);
        mOutputFile = new File(outputFile);
        mCodeTable = new HashMap<>();
    }

    public void encode() throws IOException {
        FileInputStream fileInputStream = new FileInputStream(mInputFile);
        FileOutputStream fileOutputStream = new FileOutputStream(mOutputFile);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
        DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);
        DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);
        readCodeTable(dataInputStream);
        readContent(dataInputStream, dataOutputStream);
        bufferedOutputStream.flush();
        fileInputStream.close();
        fileOutputStream.close();
    }

    public static void main(String[] args) throws IOException {
        AlgorithmShennonaEncode algorithmShennonaEncode = new AlgorithmShennonaEncode("E:\\Java\\fileOutput.khan", "E:\\Java\\decode.txt");
        algorithmShennonaEncode.encode();
    }

    private int mIndex;

    private void readCodeTable(DataInputStream dataInputStream) throws IOException {
        int countSymbol = dataInputStream.readInt();
        for (int i = 0; i < countSymbol; i++) {
            /* 1 2 3 4 5 6 9 7 8
            2*i+1 - zero
            2*i+2 - unit
             */
            byte symbol = dataInputStream.readByte();
            int countBit = dataInputStream.readByte();

            int countByte = (int) Math.ceil((double) countBit / 8);
            StringBuilder code = new StringBuilder();
            int raiseBit = countBit;
            for (int k = 0; k < countByte; k++) {
                byte readByte = dataInputStream.readByte();
                String binary = Integer.toBinaryString(readByte & mByteMask);
                StringBuilder stringBuilder = new StringBuilder(binary);
                if (raiseBit >= 8) {
                    code.append(stringBuilder.substring(stringBuilder.length() - 8, stringBuilder.length()));
                    stringBuilder.delete(stringBuilder.length() - 8, stringBuilder.length());
                    raiseBit -= 8;
                } else {
                    int end=stringBuilder.length();
                    if (stringBuilder.length() < raiseBit) {
                        for (int j = 0; j < Math.abs(end - raiseBit); j++)
                            stringBuilder.insert(0, "0");
                    }
                    code.append(stringBuilder.substring(stringBuilder.length() - raiseBit, stringBuilder.length()));
                }
            }
            mCodeTable.put(symbol, code.toString());
        }
    }

    // -1 - 110001 -2 - 1110101 - 3 -111100011 4 -110011
    private Observable<Character> createBitObservable(String bytes) {
        return Observable.create(observableEmitter -> {
            for (int i = 0; i < bytes.length(); i++) {
                char s = bytes.charAt(i);
                observableEmitter.onNext(s);
            }
        });
    }

    private Observable<Character> createBitObservableFromInputStream(DataInputStream dataInputStream) {
        return Observable
                .create(observableEmitter -> {
                    int currentByte;
                    Deque<Byte> deque = new LinkedList<>();
                    for (int i = 0; i < 3; i++) {
                        if ((currentByte = dataInputStream.read()) != -1) {
                            deque.push((byte) currentByte);
                            //System.out.println((byte) currentByte);
                        }
                    }
                    while (true) {
                        if ((currentByte = dataInputStream.read()) != -1) {
                            byte b = deque.pollLast();
                            for (int mask : sBitMasks) {
                                observableEmitter.onNext((b & mask) == 0 ? '0' : '1');
                            }
                            deque.push((byte) currentByte);
                            //System.out.println((byte) currentByte);
                        } else {
                            while (deque.size()>2){
                                byte b = deque.pollLast();
                                for (int mask : sBitMasks) {
                                    observableEmitter.onNext((b & mask) == 0 ? '0' : '1');
                                }
                            }
                            byte lastSymbol = deque.pollLast();
                            byte postfixEmptyCount = deque.pollLast();
                            for (int i = 0; i < 8 - postfixEmptyCount; i++) {
                                observableEmitter.onNext((lastSymbol & sBitMasks[i]) == 0 ? '0' : '1');
                            }
                            break;
                        }
                    }
                });
    }

    private void readContent(DataInputStream dataInputStream, DataOutputStream dataOutputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        createBitObservableFromInputStream(dataInputStream)
                .subscribe(character -> {
                    stringBuilder.append(character);
                    String code = stringBuilder.toString();
                    for (Map.Entry<Byte, String> entry : mCodeTable.entrySet()) {
                        if (code.equals(entry.getValue())) {
                            dataOutputStream.writeByte(entry.getKey());
                            stringBuilder.setLength(0);
                            break;
                        }
                    }
                },throwable -> throwable.printStackTrace(),() -> {});
    }

    private void computeIndex(char b) {
        if (b == '1') {
            if (mIndex == -1)
                mIndex = 1;
            else
                mIndex = 2 * mIndex + 1;
        } else if (b == '0') {
            if (mIndex == -1)
                mIndex = 2;
            else
                mIndex = 2 * mIndex + 2;
        }
    }

    /*
    private int mCountOfBitInWriteByte;
    private int mWriteByte;
    private void writeBitByBytes(int[] bytes,int countBit,DataOutputStream dataOutputStream) throws IOException {
        int targetCountBit=countBit;
        for (int i = 0; i < bytes.length; i++) {
            byte targetByte = (byte) bytes[i];

            if (targetCountBit < 8) {

                int emptyPrefix = 8 - targetCountBit;
                targetByte<<=emptyPrefix;

                if (mCountOfBitInWriteByte != 0) {
                    if (mCountOfBitInWriteByte < 8) {
                        int balanceCountBit=8- mCountOfBitInWriteByte;
                        if(targetCountBit<balanceCountBit){
                            byte balanceByte= (byte) (targetByte<<8- mCountOfBitInWriteByte -targetCountBit);
                            mWriteByte |=balanceByte;
                            mCountOfBitInWriteByte +=targetCountBit;
                        }else if(targetCountBit==balanceCountBit){
                            byte balanceByte= (byte) (targetByte>>>8-targetCountBit);
                            mWriteByte &=AlgorithmShennonaCode.sMaskStart[mCountOfBitInWriteByte];
                            mWriteByte |=balanceByte;
                            dataOutputStream.write(mWriteByte);
                            mCountOfBitInWriteByte =0;
                        }else{
                            int balanceByte=targetByte&AlgorithmShennonaCode.sMaskStart[balanceCountBit];
                            balanceByte>>>=8-balanceCountBit;
                            mWriteByte &=AlgorithmShennonaCode.sMaskStart[mCountOfBitInWriteByte];
                            mWriteByte |=balanceByte;
                            dataOutputStream.write(mWriteByte);
                            targetByte&=AlgorithmShennonaCode.sMaskStart[targetCountBit];
                            targetByte<<=balanceCountBit;
                            mWriteByte = targetByte;
                            mCountOfBitInWriteByte =targetCountBit-balanceCountBit;
                        }

                    } else {
                        dataOutputStream.write(mWriteByte);
                        mWriteByte = targetByte;
                        mCountOfBitInWriteByte =targetCountBit;
                    }
                } else {
                    mWriteByte =targetByte;
                    mCountOfBitInWriteByte = targetCountBit;
                }
                break;
            } else {
                if (mCountOfBitInWriteByte != 0) {
                    if (mCountOfBitInWriteByte < 8) {
                        int balanceCountBit = 8 - mCountOfBitInWriteByte;
                        int balanceByte = targetByte & AlgorithmShennonaCode.sMaskStart[balanceCountBit];
                        balanceByte >>>= 8 - balanceCountBit;
                        mWriteByte &= AlgorithmShennonaCode.sMaskStart[mCountOfBitInWriteByte];
                        mWriteByte |= balanceByte;
                        dataOutputStream.write(mWriteByte);
                        mCountOfBitInWriteByte = 8 - balanceCountBit;
                        mWriteByte <<= balanceCountBit;
                    } else {
                        dataOutputStream.write(mWriteByte);
                        dataOutputStream.write(targetByte);
                        mCountOfBitInWriteByte = 0;
                    }
                } else {
                    dataOutputStream.write(targetByte);
                    mCountOfBitInWriteByte =0;
                }
                targetCountBit -= 8;
            }
        }
    }
*/
    private Byte getSymbol(byte b) {
        if (b == 1) {

        } else if (b == 0) {

        }
        return null;
    }

    private static class Node {

        private Byte mSymbol = null;
        private Byte mBit = null;

        public Node(Byte symbol, Byte bit) {
            mSymbol = symbol;
            mBit = bit;
        }
    }
}
