#ifndef RINGBUFFER_H
#define RINGBUFFER_H

#include <vector>
#include <mutex>
#include <condition_variable>
#include <optional>
#include <functional>

template <typename T>
class RingBuffer {
public:
    explicit RingBuffer(size_t capacity, std::function<void(T)> cleanup_func = nullptr)
        : capacity_(capacity), head_(0), tail_(0), size_(0), cleanup_(cleanup_func) {
        buffer_.resize(capacity);
    }

    ~RingBuffer() {
        clear();
    }

    void push(T item) {
        std::unique_lock<std::mutex> lock(mutex_);
        cond_full_.wait(lock, [this]() { return size_ < capacity_; });

        if (size_ == capacity_) {
            // This case should technically not happen because of wait(),
            // but for safety in non-blocking variants:
            if (cleanup_) cleanup_(buffer_[tail_]);
        }

        buffer_[tail_] = item;
        tail_ = (tail_ + 1) % capacity_;
        size_++;

        lock.unlock();
        cond_empty_.notify_one();
    }

    T pop() {
        std::unique_lock<std::mutex> lock(mutex_);
        cond_empty_.wait(lock, [this]() { return size_ > 0; });

        T item = buffer_[head_];
        head_ = (head_ + 1) % capacity_;
        size_--;

        lock.unlock();
        cond_full_.notify_one();
        return item;
    }

    void clear() {
        std::lock_guard<std::mutex> lock(mutex_);
        while (size_ > 0) {
            T item = buffer_[head_];
            if (cleanup_) cleanup_(item);
            head_ = (head_ + 1) % capacity_;
            size_--;
        }
    }

    bool isEmpty() {
        std::lock_guard<std::mutex> lock(mutex_);
        return size_ == 0;
    }

    size_t size() {
        std::lock_guard<std::mutex> lock(mutex_);
        return size_;
    }

private:
    size_t capacity_;
    std::vector<T> buffer_;
    size_t head_;
    size_t tail_;
    size_t size_;

    std::mutex mutex_;
    std::condition_variable cond_empty_;
    std::condition_variable cond_full_;
    std::function<void(T)> cleanup_;
};

#endif // RINGBUFFER_H
