package coder;

import io.reactivex.Observable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;

public class Encoder {

    private static int mByteMask = Integer.parseInt("11111111", 2);


    private static int[] sBitMasks = new int[]{
            Integer.parseInt("10000000", 2),
            Integer.parseInt("01000000", 2),
            Integer.parseInt("00100000", 2),
            Integer.parseInt("00010000", 2),
            Integer.parseInt("00001000", 2),
            Integer.parseInt("00000100", 2),
            Integer.parseInt("00000010", 2),
            Integer.parseInt("00000001", 2),

    private int mCountSymbol;
    private int mByCountSymbol;
    private Map<Coder.CodeSymbol, String> mCodeTable;

    private void readCodeTable(DataInputStream dataInputStream) throws IOException {
        mCountSymbol = dataInputStream.readInt();
        mByCountSymbol = dataInputStream.readByte();
        for (int i = 0; i < mCountSymbol; i++) {
            byte[] symbol = new byte[mByCountSymbol];
            for (int k = 0; k < mByCountSymbol; k++)
                symbol[k] = dataInputStream.readByte();
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
                    int end = stringBuilder.length();
                    if (stringBuilder.length() < raiseBit) {
                        for (int j = 0; j < Math.abs(end - raiseBit); j++)
                            stringBuilder.insert(0, "0");
                    }
                    code.append(stringBuilder.substring(stringBuilder.length() - raiseBit, stringBuilder.length()));
                }
            }
            Coder.CodeSymbol codeSymbol = new Coder.CodeSymbol(symbol);
            mCodeTable.put(codeSymbol, code.toString());
        }
    }


    private Observable<Character> createBitObservableFromInputStream(DataInputStream dataInputStream,DataOutputStream dataOutputStream) {
        return Observable
                .create(observableEmitter -> {
                    int currentByte;
                    Deque<Byte> deque = new LinkedList<>();
                    for (int i = 0; i < mCountSymbol+1; i++) {
                        if ((currentByte = dataInputStream.read()) != -1) {
                            deque.push((byte) currentByte);
                        }
                    }
                    while (true) {
                        if ((currentByte = dataInputStream.read()) != -1) {
                            byte b = deque.pollLast();
                            for (int mask : sBitMasks) {
                                observableEmitter.onNext((b & mask) == 0 ? '0' : '1');
                            }
                            deque.push((byte) currentByte);
                        } else {
                            byte lastSymbol = deque.pollLast();
                            byte postfixEmptyCount = deque.pollLast();
                            for (int i = 0; i < 8 - postfixEmptyCount; i++) {
                                observableEmitter.onNext((lastSymbol & sBitMasks[i]) == 0 ? '0' : '1');
                            }
                            for(int i=0;i<mCountSymbol-1;i++){
                                dataOutputStream.writeByte(deque.pollLast());
                            }
                            break;
                        }
                    }
                });
    }

    private void readContent(DataInputStream dataInputStream, DataOutputStream dataOutputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        createBitObservableFromInputStream(dataInputStream,dataOutputStream)
                .subscribe(character -> {
                    stringBuilder.append(character);
                    String code = stringBuilder.toString();
                    for (Map.Entry<Coder.CodeSymbol, String> entry : mCodeTable.entrySet()) {
                        if (code.equals(entry.getValue())) {
                            dataOutputStream.write(entry.getKey().getBytes());
                            stringBuilder.setLength(0);
                            break;
                        }
                    }
                },throwable -> throwable.printStackTrace(),() -> {});
    }


}
