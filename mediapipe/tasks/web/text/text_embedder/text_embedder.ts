/**
 * Copyright 2022 The MediaPipe Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {CalculatorGraphConfig} from '../../../../framework/calculator_pb';
import {CalculatorOptions} from '../../../../framework/calculator_options_pb';
import {EmbeddingResult} from '../../../../tasks/cc/components/containers/proto/embeddings_pb';
import {BaseOptions as BaseOptionsProto} from '../../../../tasks/cc/core/proto/base_options_pb';
import {TextEmbedderGraphOptions as TextEmbedderGraphOptionsProto} from '../../../../tasks/cc/text/text_embedder/proto/text_embedder_graph_options_pb';
import {Embedding} from '../../../../tasks/web/components/containers/embedding_result';
import {convertEmbedderOptionsToProto} from '../../../../tasks/web/components/processors/embedder_options';
import {convertFromEmbeddingResultProto} from '../../../../tasks/web/components/processors/embedder_result';
import {computeCosineSimilarity} from '../../../../tasks/web/components/utils/cosine_similarity';
import {TaskRunner} from '../../../../tasks/web/core/task_runner';
import {WasmFileset} from '../../../../tasks/web/core/wasm_fileset';
import {WasmModule} from '../../../../web/graph_runner/graph_runner';
// Placeholder for internal dependency on trusted resource url

import {TextEmbedderOptions} from './text_embedder_options';
import {TextEmbedderResult} from './text_embedder_result';

export * from './text_embedder_options';
export * from './text_embedder_result';

// The OSS JS API does not support the builder pattern.
// tslint:disable:jspb-use-builder-pattern

const INPUT_STREAM = 'text_in';
const EMBEDDINGS_STREAM = 'embeddings_out';
const TEXT_EMBEDDER_CALCULATOR =
    'mediapipe.tasks.text.text_embedder.TextEmbedderGraph';

/**
 * Performs embedding extraction on text.
 */
export class TextEmbedder extends TaskRunner<TextEmbedderOptions> {
  private embeddingResult: TextEmbedderResult = {embeddings: []};
  private readonly options = new TextEmbedderGraphOptionsProto();

  /**
   * Initializes the Wasm runtime and creates a new text embedder from the
   * provided options.
   * @param wasmFileset A configuration object that provides the location of the
   *     Wasm binary and its loader.
   * @param textEmbedderOptions The options for the text embedder. Note that
   *     either a path to the TFLite model or the model itself needs to be
   *     provided (via `baseOptions`).
   */
  static createFromOptions(
      wasmFileset: WasmFileset,
      textEmbedderOptions: TextEmbedderOptions): Promise<TextEmbedder> {
    return TaskRunner.createInstance(
        TextEmbedder, /* initializeCanvas= */ false, wasmFileset,
        textEmbedderOptions);
  }

  /**
   * Initializes the Wasm runtime and creates a new text embedder based on the
   * provided model asset buffer.
   * @param wasmFileset A configuration object that provides the location of the
   *     Wasm binary and its loader.
   * @param modelAssetBuffer A binary representation of the TFLite model.
   */
  static createFromModelBuffer(
      wasmFileset: WasmFileset,
      modelAssetBuffer: Uint8Array): Promise<TextEmbedder> {
    return TaskRunner.createInstance(
        TextEmbedder, /* initializeCanvas= */ false, wasmFileset,
        {baseOptions: {modelAssetBuffer}});
  }

  /**
   * Initializes the Wasm runtime and creates a new text embedder based on the
   * path to the model asset.
   * @param wasmFileset A configuration object that provides the location of the
   *     Wasm binary and its loader.
   * @param modelAssetPath The path to the TFLite model.
   */
  static createFromModelPath(
      wasmFileset: WasmFileset,
      modelAssetPath: string): Promise<TextEmbedder> {
    return TaskRunner.createInstance(
        TextEmbedder, /* initializeCanvas= */ false, wasmFileset,
        {baseOptions: {modelAssetPath}});
  }

  constructor(
      wasmModule: WasmModule,
      glCanvas?: HTMLCanvasElement|OffscreenCanvas|null) {
    super(wasmModule, glCanvas);
    this.options.setBaseOptions(new BaseOptionsProto());
  }

  /**
   * Sets new options for the text embedder.
   *
   * Calling `setOptions()` with a subset of options only affects those options.
   * You can reset an option back to its default value by explicitly setting it
   * to `undefined`.
   *
   * @param options The options for the text embedder.
   */
  override async setOptions(options: TextEmbedderOptions): Promise<void> {
    await super.setOptions(options);
    this.options.setEmbedderOptions(convertEmbedderOptionsToProto(
        options, this.options.getEmbedderOptions()));
    this.refreshGraph();
  }

  protected override get baseOptions(): BaseOptionsProto {
    return this.options.getBaseOptions()!;
  }

  protected override set baseOptions(proto: BaseOptionsProto) {
    this.options.setBaseOptions(proto);
  }

  /**
   * Performs embeding extraction on the provided text and waits synchronously
   * for the response.
   *
   * @param text The text to process.
   * @return The embedding resuls of the text
   */
  embed(text: string): TextEmbedderResult {
    // Get text embeddings by running our MediaPipe graph.
    this.graphRunner.addStringToStream(
        text, INPUT_STREAM, /* timestamp= */ performance.now());
    this.finishProcessing();
    return this.embeddingResult;
  }

  /**
   * Utility function to compute cosine similarity[1] between two `Embedding`
   * objects.
   *
   * [1]: https://en.wikipedia.org/wiki/Cosine_similarity
   *
   * @throws if the embeddings are of different types(float vs. quantized), have
   *     different sizes, or have an L2-norm of 0.
   */
  static cosineSimilarity(u: Embedding, v: Embedding): number {
    return computeCosineSimilarity(u, v);
  }

  /** Updates the MediaPipe graph configuration. */
  private refreshGraph(): void {
    const graphConfig = new CalculatorGraphConfig();
    graphConfig.addInputStream(INPUT_STREAM);
    graphConfig.addOutputStream(EMBEDDINGS_STREAM);

    const calculatorOptions = new CalculatorOptions();
    calculatorOptions.setExtension(
        TextEmbedderGraphOptionsProto.ext, this.options);

    const embedderNode = new CalculatorGraphConfig.Node();
    embedderNode.setCalculator(TEXT_EMBEDDER_CALCULATOR);
    embedderNode.addInputStream('TEXT:' + INPUT_STREAM);
    embedderNode.addOutputStream('EMBEDDINGS:' + EMBEDDINGS_STREAM);
    embedderNode.setOptions(calculatorOptions);

    graphConfig.addNode(embedderNode);

    this.graphRunner.attachProtoListener(EMBEDDINGS_STREAM, binaryProto => {
      const embeddingResult = EmbeddingResult.deserializeBinary(binaryProto);
      this.embeddingResult = convertFromEmbeddingResultProto(embeddingResult);
    });

    const binaryGraph = graphConfig.serializeBinary();
    this.setGraph(new Uint8Array(binaryGraph), /* isBinary= */ true);
  }
}



