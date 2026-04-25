<script lang="ts">
  import { Dropzone, Spinner } from 'flowbite-svelte';
  import { TrashBinSolid, CloudArrowUpOutline } from 'flowbite-svelte-icons';
  import { apiClient, ProblemException, type OwnCarListing } from '$lib/apiClient';
  import { t } from '$lib/i18n';
  import SparkMD5 from 'spark-md5';

  let {
    listing = $bindable(),
    onchange,
  }: {
    listing: OwnCarListing;
    onchange: () => void;
  } = $props();

  const MAX_IMAGES = 10;
  const MAX_FILE_SIZE = 10 * 1024 * 1024;
  const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp'] as const;

  let uploading = $state(false);
  let uploadError = $state<string | null>(null);
  let dragSourceIdx = $state<number | null>(null);
  let dragOverIdx = $state<number | null>(null);

  const images = $derived(listing.images ?? []);

  function computeMD5Base64(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        const spark = new SparkMD5.ArrayBuffer();
        spark.append(e.target!.result as ArrayBuffer);
        resolve(btoa(spark.end(true)));
      };
      reader.onerror = reject;
      reader.readAsArrayBuffer(file);
    });
  }

  async function uploadFile(file: File): Promise<string | null> {
    if (!ALLOWED_TYPES.includes(file.type as (typeof ALLOWED_TYPES)[number])) {
      return $t('edit.images.invalidType');
    }
    if (file.size > MAX_FILE_SIZE) {
      return $t('edit.images.tooLarge');
    }

    const md5 = await computeMD5Base64(file);

    const signedUrl = await apiClient.generateSignedUrl({
      listingId: listing.id,
      contentLength: file.size,
      md5,
      contentType: file.type as (typeof ALLOWED_TYPES)[number],
    });

    const headers: Record<string, string> = {};
    if (signedUrl.uploadHeaders) {
      for (const [key, values] of Object.entries(signedUrl.uploadHeaders)) {
        headers[key] = values.join(', ');
      }
    }

    const res = await fetch(signedUrl.uploadUrl, { method: 'PUT', headers, body: file });
    if (!res.ok) throw new Error('Upload failed');

    listing.images = [...(listing.images ?? []), { key: signedUrl.fileKey, url: signedUrl.fileUrl }];
    onchange();
    return null;
  }

  let dropzoneFiles = $state<FileList | null>(null);

  $effect(() => {
    const files = dropzoneFiles;
    if (!files?.length) return;
    dropzoneFiles = null;
    processFiles(files);
  });

  async function processFiles(fileList: FileList) {
    const slots = MAX_IMAGES - images.length;
    if (slots <= 0) {
      uploadError = $t('edit.images.maxReached');
      return;
    }

    uploadError = null;
    uploading = true;

    try {
      for (const file of Array.from(fileList).slice(0, slots)) {
        const err = await uploadFile(file);
        if (err) { uploadError = err; break; }
      }
    } catch (e) {
      uploadError = e instanceof ProblemException
        ? e.problem.title
        : $t('edit.images.uploadFailed');
    } finally {
      uploading = false;
    }
  }

  function deleteImage(idx: number) {
    listing.images = images.filter((_, i) => i !== idx);
    onchange();
  }

  function onDragStart(idx: number, e: DragEvent) {
    dragSourceIdx = idx;
    e.dataTransfer!.effectAllowed = 'move';
  }

  function onDragOver(e: DragEvent, idx: number) {
    e.preventDefault();
    e.dataTransfer!.dropEffect = 'move';
    dragOverIdx = idx;
  }

  function reorderImage(from: number, to: number) {
    const reordered = [...images];
    const [moved] = reordered.splice(from, 1);
    reordered.splice(to, 0, moved);
    listing.images = reordered;
    onchange();
  }

  function onDrop(e: DragEvent, targetIdx: number) {
    e.preventDefault();
    if (dragSourceIdx !== null && dragSourceIdx !== targetIdx) {
      reorderImage(dragSourceIdx, targetIdx);
    }
    dragSourceIdx = null;
    dragOverIdx = null;
  }

  function onDragEnd() {
    dragSourceIdx = null;
    dragOverIdx = null;
  }
</script>

<div class="space-y-4">
  {#if images.length > 0}
    <div class="grid grid-cols-2 sm:grid-cols-3 gap-3">
      {#each images as image, i (image.key)}
        <div
          class="relative group aspect-4/3 rounded-xl overflow-hidden border-2 cursor-grab active:cursor-grabbing transition-colors"
          class:border-blue-400={dragOverIdx === i && dragSourceIdx !== i}
          class:border-transparent={!(dragOverIdx === i && dragSourceIdx !== i)}
          class:opacity-40={dragSourceIdx === i}
          role="button"
          tabindex="0"
          aria-label="Car image {i + 1}, drag to reorder"
          draggable="true"
          ondragstart={(e) => onDragStart(i, e)}
          ondragover={(e) => onDragOver(e, i)}
          ondrop={(e) => onDrop(e, i)}
          ondragend={onDragEnd}
          onkeydown={(e) => {
            if (e.key === 'ArrowLeft' && i > 0) reorderImage(i, i - 1);
            if (e.key === 'ArrowRight' && i < images.length - 1) reorderImage(i, i + 1);
          }}
        >
          {#if image.url}
            <img
              src={image.url}
              alt="Car image {i + 1}"
              class="w-full h-full object-cover pointer-events-none"
              draggable="false"
            />
          {:else}
            <div class="w-full h-full bg-gray-100 dark:bg-gray-700 flex items-center justify-center">
              <span class="text-xs text-muted">{$t('edit.images.processing')}</span>
            </div>
          {/if}

          {#if i === 0}
            <span class="absolute top-1.5 left-1.5 bg-black/60 text-white text-xs px-1.5 py-0.5 rounded-md select-none">
              {$t('edit.images.cover')}
            </span>
          {/if}

          <button
            class="absolute top-1.5 right-1.5 bg-red-500 hover:bg-red-600 text-white rounded-md p-1 opacity-0 group-hover:opacity-100 transition-opacity"
            type="button"
            aria-label={$t('edit.images.delete')}
            onclick={() => deleteImage(i)}
          >
            <TrashBinSolid class="w-3.5 h-3.5" />
          </button>
        </div>
      {/each}
    </div>
  {/if}

  {#if images.length < MAX_IMAGES}
    <Dropzone
      accept="image/jpeg,image/png,image/webp"
      multiple
      disabled={uploading}
      bind:files={dropzoneFiles}
      class="h-auto py-5"
    >
      {#if uploading}
        <Spinner size="8" />
        <p class="mt-3 text-sm text-muted">{$t('edit.images.uploading')}</p>
      {:else}
        <CloudArrowUpOutline class="mb-3 h-10 w-10 text-gray-400" />
        <p class="mb-1 text-sm text-gray-500 dark:text-gray-400">
          <span class="font-semibold">{$t('edit.images.dropClick')}</span>
          {$t('edit.images.dropDrag')}
        </p>
        <p class="text-xs text-muted">{$t('edit.images.hint')} · {images.length}/{MAX_IMAGES}</p>
      {/if}
    </Dropzone>

    {#if uploadError}
      <p class="text-xs text-red-500">{uploadError}</p>
    {/if}
  {:else}
    <p class="text-xs text-muted">{$t('edit.images.maxReached')}</p>
  {/if}
</div>
