package com.github.ikeras;

import java.util.Arrays;
import java.util.Stack;

public class CPU {
    private final byte SMALL_FONT_HEIGHT = 5;
    private final byte LARGE_FONT_HEIGHT = 10;
    private final short SMALL_FONT_MEMORY_OFFSET = 0x00;
    private final short LARGE_FONT_MEMORY_OFFSET = 0x50;

    private static final short[] SMALL_FONT = new short[] { 
        0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
        0x20, 0x60, 0x20, 0x20, 0x70, // 1
        0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
        0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
        0x90, 0x90, 0xF0, 0x10, 0x10, // 4
        0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
        0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
        0xF0, 0x10, 0x20, 0x40, 0x40, // 7
        0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
        0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
        0xF0, 0x90, 0xF0, 0x90, 0x90, // A
        0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
        0xF0, 0x80, 0x80, 0x80, 0xF0, // C
        0xE0, 0x90, 0x90, 0x90, 0xE0, // D
        0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
        0xF0, 0x80, 0xF0, 0x80, 0x80 // F
    };

    private static final short[] LARGE_FONT = new short[] { 
        0x7C, 0x82, 0x82, 0x82, 0x82, 0x82, 0x82, 0x82, 0x7C, 0x00, // 0
        0x08, 0x18, 0x38, 0x08, 0x08, 0x08, 0x08, 0x08, 0x3C, 0x00, // 1
        0x7C, 0x82, 0x02, 0x02, 0x04, 0x18, 0x20, 0x40, 0xFE, 0x00, // 2
        0x7C, 0x82, 0x02, 0x02, 0x3C, 0x02, 0x02, 0x82, 0x7C, 0x00, // 3
        0x84, 0x84, 0x84, 0x84, 0xFE, 0x04, 0x04, 0x04, 0x04, 0x00, // 4
        0xFE, 0x80, 0x80, 0x80, 0xFC, 0x02, 0x02, 0x82, 0x7C, 0x00, // 5
        0x7C, 0x82, 0x80, 0x80, 0xFC, 0x82, 0x82, 0x82, 0x7C, 0x00, // 6
        0xFE, 0x02, 0x04, 0x08, 0x10, 0x20, 0x20, 0x20, 0x20, 0x00, // 7
        0x7C, 0x82, 0x82, 0x82, 0x7C, 0x82, 0x82, 0x82, 0x7C, 0x00, // 8
        0x7C, 0x82, 0x82, 0x82, 0x7E, 0x02, 0x02, 0x82, 0x7C, 0x00, // 9
        0x10, 0x28, 0x44, 0x82, 0x82, 0xFE, 0x82, 0x82, 0x82, 0x00, // A
        0xFC, 0x82, 0x82, 0x82, 0xFC, 0x82, 0x82, 0x82, 0xFC, 0x00, // B
        0x7C, 0x82, 0x80, 0x80, 0x80, 0x80, 0x80, 0x82, 0x7C, 0x00, // C
        0xFC, 0x82, 0x82, 0x82, 0x82, 0x82, 0x82, 0x82, 0xFC, 0x00, // D
        0xFE, 0x80, 0x80, 0x80, 0xF8, 0x80, 0x80, 0x80, 0xFE, 0x00, // E
        0xFE, 0x80, 0x80, 0x80, 0xF8, 0x80, 0x80, 0x80, 0x80, 0x00, // F
    };

    private int _displayWidth;
    private int _displayHeight;
    private byte[] _display;
    private final boolean[] _keys;
    private final short[] _memory;

    private final short[] _persistedRegisters;
    private final short[] _registers;
    private int _i;
    private int _lastKeyPressed;
    private int _numberOfKeysPressed;
    private short _soundTimer;
    private short _delayTimer;
    private final Stack<Integer> _stack;
    private int _pc;

    public CPU(short[] memory) {
        _memory = memory;
        _keys = new boolean[16];
        _persistedRegisters = new short[16];
        _registers = new short[16];
        _stack = new Stack<Integer>();
        _soundTimer = 0;
        _delayTimer = 0;
        _lastKeyPressed = 0x0;
        _numberOfKeysPressed = 0;
        _i = 0;
        _pc = 0x200;

        // We don't call createDisplay() here because we cannot synchronize on _display until after it's been initialized once
        int width = 64;
        int height = 32;
        _display = new byte[width * height];
        _displayWidth = width;
        _displayHeight = height;

        System.arraycopy(SMALL_FONT, 0, _memory, SMALL_FONT_MEMORY_OFFSET, SMALL_FONT.length);
        System.arraycopy(LARGE_FONT, 0, _memory, LARGE_FONT_MEMORY_OFFSET, LARGE_FONT.length);
    }

    byte[] getDisplay() {
        synchronized (_display) {
            return Arrays.copyOf(_display, _display.length);
        }
    }

    int getDisplayHeight() {
        return _displayHeight;
    }

    int getDisplayWidth() {
        return _displayWidth;
    }

    void executeNextInstruction() {
        short hiInstruction = _memory[_pc];
        short loInstruction = _memory[_pc + 1];
        _pc += 2;

        byte instruction = (byte)(hiInstruction >> 4);
        byte x = (byte)(hiInstruction & 0x0f);
        byte y = (byte)(loInstruction >> 4);
        byte n = (byte)(loInstruction & 0x0f);

        short kk = loInstruction;
        short nnn = (short)(x << 8 | kk);

        switch (instruction) {
            case 0x0:
                if (nnn == 0x0e0) {
                    synchronized (_display) {
                        Arrays.fill(_display, (byte)0);
                    }
                } else if (nnn >= 0x00c0 && nnn <= 0x00cf) {
                    scrollDown(n);
                } else if (nnn == 0x00ee) {
                    _pc = _stack.pop();
                } else if (nnn == 0x00fb) {
                    scrollRight();
                } else if (nnn == 0x00fc) {
                    scrollLeft();
                } else if (nnn == 0x00fe) {
                    createDisplay(64, 32);
                } else if (nnn == 0x00ff) {
                    createDisplay(128, 64);
                }
                break;
            case 0x1:
                _pc = nnn;
                break;
            case 0x2:
                _stack.push(_pc);
                _pc = nnn;
                break;
            case 0x3:
                if (_registers[x] == kk) {
                    _pc += 2;
                }
                break;
            case 0x4:
                if (_registers[x] != kk) {
                    _pc += 2;
                }
                break;
            case 0x5:
                if (_registers[x] == _registers[y]) {
                    _pc += 2;
                }
                break;
            case 0x6:
                _registers[x] = kk;
                break;
            case 0x7:
                _registers[x] = (short)((_registers[x] + kk) & 0xff);
                break;
            case 0x8:
                switch (n) {
                    case 0x0:
                        _registers[x] = _registers[y];
                        break;
                    case 0x1:
                        _registers[x] |= _registers[y];
                        break;
                    case 0x2:
                        _registers[x] &= _registers[y];
                        break;
                    case 0x3:
                        _registers[x] ^= _registers[y];
                        break;
                    case 0x4:
                        int sum = _registers[x] + _registers[y];
                        _registers[0xf] = (short)(sum > 0xff ? 1 : 0);
                        _registers[x] = (short)(sum & 0xff);
                        break;
                    case 0x5:
                        _registers[0xf] = (short)(_registers[x] >= _registers[y] ? 1 : 0);
                        _registers[x] = (short)((_registers[x] - _registers[y]) & 0xff);
                        break;
                    case 0x6:
                        _registers[0xf] = (short)(_registers[x] & 0x1);
                        _registers[x] = (short)(_registers[x] >> 1);
                        break;
                    case 0x7:
                        _registers[0xf] = (short)(_registers[y] >= _registers[x] ? 1 : 0);
                        _registers[x] = (short)((_registers[y] - _registers[x]) & 0xff);
                        break;
                    case 0xe:
                        _registers[0xf] = (short)(_registers[x] >> 0x7);
                        _registers[x] = (short)((_registers[x] << 1) & 0xff);
                        break;
                    default:
                        throw new RuntimeException("Unknown instruction: " + Integer.toHexString(hiInstruction) + Integer.toHexString(loInstruction));
                }
                break;
            case 0x9:
                if (_registers[x] != _registers[y]) {
                    _pc += 2;
                }
                break;
            case 0xa:
                _i = nnn;
                break;
            case 0xb:
                _pc = (int)((_registers[0] + nnn) & 0xffff);
                break;
            case 0xc:
                _registers[x] = (short)((short)(Math.random() * 0xff) & kk);
                break;
            case 0xd:
                drawSprite(x, y, n);
                break;
            case 0xe:
                if (kk == 0x9e) {
                    if (_keys[_registers[x]]) {
                        _pc += 2;
                    }
                } else if (kk == 0xa1) {
                    if (!_keys[_registers[x]]) {
                        _pc += 2;
                    }
                } else {
                    throw new RuntimeException("Unknown instruction: " + Integer.toHexString(hiInstruction) + Integer.toHexString(loInstruction));
                }
                break;
            case 0xf:
                switch (kk) {
                    case 0x07:
                        _registers[x] = _delayTimer;
                        break;
                    case 0x0a:
                        if (_numberOfKeysPressed > 0) {
                            _registers[x] = (short)_lastKeyPressed;
                        } else {
                            _pc -= 2;
                        }
                        break;
                    case 0x15:
                        _delayTimer = _registers[x];
                        break;
                    case 0x18:
                        _soundTimer = _registers[x];
                        break;
                    case 0x1e:
                        int result = _i + _registers[x];
                        if (result > 0xfff) {
                            _registers[0xf] = 1;
                        }
                        _i = result & 0xfff;
                        break;
                    case 0x29:
                        _i = SMALL_FONT_MEMORY_OFFSET + (_registers[x] * SMALL_FONT_HEIGHT);
                        break;
                    case 0x30:
                        _i = LARGE_FONT_MEMORY_OFFSET + (_registers[x] * LARGE_FONT_HEIGHT);
                        break;
                    case 0x33:
                        _memory[_i] = (byte)(_registers[x] / 100);
                        _memory[_i + 1] = (byte)((_registers[x] / 10) % 10);
                        _memory[_i + 2] = (byte)(_registers[x] % 10);
                        break;
                    case 0x55:
                        System.arraycopy(_registers, 0, _memory, _i, x + 1);
                        break;
                    case 0x65:
                        System.arraycopy(_memory, _i, _registers, 0, x + 1);
                        break;
                    case 0x75:
                        System.arraycopy(_registers, 0, _persistedRegisters, 0, 16);
                        break;
                    case 0x85:
                        System.arraycopy(_persistedRegisters, 0, _registers, 0, 16);
                        break;
                    default:
                        throw new RuntimeException("Unknown instruction: " + Integer.toHexString(hiInstruction) + Integer.toHexString(loInstruction));
                }
                break;
        }
    }

    void pressKey(int key) {
        if (!_keys[key]) {
            _keys[key] = true;
            _lastKeyPressed = key;
            _numberOfKeysPressed++;
        }
    }

    void releaseKey(int key) {
        if (_keys[key]) {
            _keys[key] = false;
            _numberOfKeysPressed--;
        }
    }

    void tick() {
        if (_delayTimer > 0) {
            _delayTimer--;
        }

        if (_soundTimer > 0) {
            _soundTimer--;
        }
    }

    private void createDisplay(int width, int height) {
        synchronized (_display) {
            _display = new byte[width * height];
            _displayWidth = width;
            _displayHeight = height;
        }
    }

    private void drawSprite(short x, short y, short n) {
        int xStart = _registers[x] % _displayWidth;
        int yOffset = _registers[y] % _displayHeight;
        _registers[0xf] = 0;

        int spriteWidth = n == 0 ? 16 : 8;
        int sprintHeight = n == 0 ? 16 : n;

        synchronized (_display) {
            for (int row = 0; row < sprintHeight; row++) {
                int spritRowData = n == 0 ?
                    _memory[_i + (row * 2)] << 8 | _memory[_i + (row * 2) + 1] :
                    _memory[_i + row];
                int xOffset = xStart;

                for (int bit = 0; bit < spriteWidth; bit++) {
                    int spriteBit = (spritRowData & (1 << (spriteWidth - 1 - bit)));
                    int location = yOffset * _displayWidth + xOffset;
                    byte pixel = _display[location];

                    if (spriteBit > 0) {
                        if (pixel != 0) {
                            pixel = 0;
                            _registers[0xf] = 1;
                        } else {
                            pixel = 1;
                        }                                                
                    }

                    _display[location] = pixel;

                    xOffset++;

                    if (xOffset >= _displayWidth) {
                        break;
                    }
                }

                yOffset++;

                if (yOffset >= _displayHeight) {
                    break;
                }
            }
        }
    }

    private void scrollDown(int rows) {
        synchronized (_display) {
            int pixelsToMove = rows * _displayWidth;
            System.arraycopy(_display, 0, _display, pixelsToMove, _display.length - pixelsToMove);
            Arrays.fill(_display, 0, pixelsToMove, (byte)0);
        }
    }

    private void scrollLeft() {
        synchronized (_display) {
            for (int row = 0; row < _displayHeight; row++) {
                int rowOffset = row * _displayWidth;
                System.arraycopy(_display, rowOffset + 4, _display, rowOffset, _displayWidth - 4);
                Arrays.fill(_display, rowOffset + _displayWidth - 4, rowOffset + _displayWidth, (byte)0);
            }
        }
    }

    private void scrollRight() {
        synchronized (_display) {
            for (int row = 0; row < _displayHeight; row++) {
                int rowOffset = row * _displayWidth;
                System.arraycopy(_display, rowOffset, _display, rowOffset + 4, _displayWidth - 4);
                Arrays.fill(_display, rowOffset, rowOffset + 4, (byte)0);
            }
        }
    }
}
