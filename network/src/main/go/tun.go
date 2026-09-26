package firestack

import (
	"errors"
	"fmt"
	"os"
	"sync"
	"sync/atomic"
)

// TunDevice wraps the Android VpnService tun0 file descriptor
type TunDevice struct {
	fd       int
	file     *os.File
	mtu      int
	closed   atomic.Bool
	readBuf  []byte
	writeMu  sync.Mutex
}

// NewTunDevice initializes a TUN device wrapper from an existing file descriptor
func NewTunDevice(fd int, mtu int) (*TunDevice, error) {
	if fd < 0 {
		return nil, errors.New("invalid file descriptor")
	}
	if mtu <= 0 {
		mtu = 1500
	}

	file := os.NewFile(uintptr(fd), fmt.Sprintf("tun%d", fd))
	if file == nil {
		return nil, fmt.Errorf("failed to open os.File from fd %d", fd)
	}

	return &TunDevice{
		fd:      fd,
		file:    file,
		mtu:     mtu,
		readBuf: make([]byte, mtu+14),
	}, nil
}

// ReadPacket reads a single raw IP packet from the TUN device
func (t *TunDevice) ReadPacket() ([]byte, error) {
	if t.closed.Load() {
		return nil, errors.New("tun device closed")
	}

	buf := make([]byte, t.mtu+14)
	n, err := t.file.Read(buf)
	if err != nil {
		return nil, err
	}
	return buf[:n], nil
}

// WritePacket writes a raw IP packet back into the TUN device
func (t *TunDevice) WritePacket(packet []byte) (int, error) {
	if t.closed.Load() {
		return 0, errors.New("tun device closed")
	}

	t.writeMu.Lock()
	defer t.writeMu.Unlock()

	return t.file.Write(packet)
}

// Close closes the underlying TUN file descriptor
func (t *TunDevice) Close() error {
	if t.closed.CompareAndSwap(false, true) {
		if t.file != nil {
			return t.file.Close()
		}
	}
	return nil
}

// Fd returns the native file descriptor number
func (t *TunDevice) Fd() int {
	return t.fd
}

// Mtu returns the configured MTU
func (t *TunDevice) Mtu() int {
	return t.mtu
}
