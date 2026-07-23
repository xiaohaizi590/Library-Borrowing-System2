<template>
  <div v-if="totalPages > 1" class="flex justify-center mt-8">
    <nav class="flex items-center space-x-2">
      <button
        @click="$emit('prev')"
        :disabled="currentPage === 0"
        class="px-3 py-2 border rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
      >
        <ChevronLeft class="w-4 h-4" />
      </button>
      <span class="px-3 py-2 text-gray-600">第 {{ currentPage + 1 }} / {{ totalPages }} 页</span>
      <button
        @click="$emit('next')"
        :disabled="currentPage >= totalPages - 1"
        class="px-3 py-2 border rounded-lg disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-50"
      >
        <ChevronRight class="w-4 h-4" />
      </button>
    </nav>
  </div>
</template>

<script setup>
import { ChevronLeft, ChevronRight } from 'lucide-vue-next'

defineProps({
  currentPage: { type: Number, required: true },
  totalPages: { type: Number, required: true }
})

defineEmits(['prev', 'next'])
</script>
