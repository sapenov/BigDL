#
# Copyright 2016 The BigDL Authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# This would makes sure Python is aware there is more than one sub-package within bigdl,
# physically located elsewhere.
# Otherwise there would be module not found error in non-pip's setting as Python would
# only search the first bigdl package and end up finding only one sub-package.

import argparse

from langchain.vectorstores import Chroma
from langchain.chains.chat_vector_db.prompts import (CONDENSE_QUESTION_PROMPT,
                                                     QA_PROMPT)
from langchain.text_splitter import CharacterTextSplitter
from langchain.chains.question_answering import load_qa_chain
from langchain.callbacks.manager import CallbackManager
from langchain.callbacks.streaming_stdout import StreamingStdOutCallbackHandler

from bigdl.llm.langchain.llms import BigdlLLM
from bigdl.llm.langchain.embeddings import BigdlLLMEmbeddings



def main(args):
    
    input_path = args.input_path 
    model_path = args.model_path 
    model_family = args.model_family
    query = args.question
    n_ctx = args.n_ctx
    n_threads=args.thread_num


    callback_manager = CallbackManager([StreamingStdOutCallbackHandler()])

    # split texts of input doc
    with open(input_path) as f:
        input_doc = f.read()
    text_splitter = CharacterTextSplitter(chunk_size=1000, chunk_overlap=0)
    texts = text_splitter.split_text(input_doc)

    # create embeddings and store into vectordb
    embeddings = BigdlLLMEmbeddings(model_path=model_path, model_family=model_family, n_threads=n_threads, n_ctx=n_ctx)
    docsearch = Chroma.from_texts(texts, embeddings, metadatas=[{"source": str(i)} for i in range(len(texts))]).as_retriever()

    #get relavant texts
    docs = docsearch.get_relevant_documents(query)
        
    bigdl_llm = BigdlLLM(
        model_path=model_path, model_family=model_family, n_ctx=n_ctx, n_threads=n_threads, callback_manager=callback_manager
    )

    doc_chain = load_qa_chain(
        bigdl_llm, chain_type="stuff", prompt=QA_PROMPT, callback_manager=callback_manager
    )

    doc_chain.run(input_documents=docs, question=query)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Llama-CPP-Python style API Simple Example')
    parser.add_argument('-x','--model-family', type=str, required=True,
                        help='the model family')
    parser.add_argument('-m','--model-path', type=str, required=True,
                        help='the path to the converted llm model')
    parser.add_argument('-i', '--input-path', type=str,
                        help='the path to the input doc.')
    parser.add_argument('-q', '--question', type=str, default='What is AI?',
                        help='qustion you want to ask.')
    parser.add_argument('-c','--n-ctx', type=int, default=2048,
                        help='number of threads to use for inference')
    parser.add_argument('-t','--thread-num', type=int, default=2,
                        help='number of threads to use for inference')
    args = parser.parse_args()
    
    main(args)
