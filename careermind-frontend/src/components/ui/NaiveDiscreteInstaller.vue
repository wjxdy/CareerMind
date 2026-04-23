<template>
  <span />
</template>

<script setup lang="ts">
import { useMessage, useDialog, useNotification, useLoadingBar } from 'naive-ui'
import { setDiscreteApi } from '@/utils/naive-discrete'

const message = useMessage()
const dialog  = useDialog()
const notification = useNotification()
const loadingBar = useLoadingBar()

setDiscreteApi({ message, dialog, notification, loadingBar })
</script>
