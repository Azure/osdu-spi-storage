// Copyright © Microsoft Corporation
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.storage.provider.azure.service;

import org.opengroup.osdu.core.common.model.http.CollaborationContext;
import org.opengroup.osdu.core.common.model.storage.Record;
import org.opengroup.osdu.core.common.model.storage.TransferInfo;
import org.opengroup.osdu.storage.provider.azure.util.RecordUtil;
import org.opengroup.osdu.storage.service.IngestionServiceImpl;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Service
@Primary
public class IngestionServiceAzureImpl extends IngestionServiceImpl {

    private final RecordUtil recordUtil;

    public IngestionServiceAzureImpl(RecordUtil recordUtil) {
        this.recordUtil = recordUtil;
    }

    @Override
    public TransferInfo createUpdateRecords(boolean skipDupes, List<Record> inputRecords, String user, Optional<CollaborationContext> collaborationContext) {
        recordUtil.validateIds(inputRecords.stream().map(Record::getId).collect(toList()));
        return super.createUpdateRecords(skipDupes, inputRecords, user, collaborationContext);
    }
}
