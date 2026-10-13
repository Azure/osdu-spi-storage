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

package org.opengroup.osdu.storage.provider.azure.generator;

import com.azure.cosmos.models.SqlQuerySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opengroup.osdu.storage.provider.azure.query.CosmosStoreQuery;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindQuerySpecGeneratorTest {

    @InjectMocks
    FindQuerySpecGenerator findQuerySpecGenerator;

    @Test
    void generateWithQueryTextShouldCreateQueryWithOrdering_whenOrderingIsPresent() {
        CosmosStoreQuery query = new CosmosStoreQuery();
        query.with(mock(Pageable.class));
        query.with(Sort.by("id")).with(Sort.by("field2"));

        String queryText = "Select id, f1, f2, f3 from c";
        String expectedQuery = "Select id, f1, f2, f3 from c ORDER BY c.field2 ASC,c.id ASC";

        SqlQuerySpec querySpec = findQuerySpecGenerator.generateWithQueryText(query, queryText);

        assertEquals(expectedQuery, querySpec.getQueryText());
    }

    @Test
    void generateWithQueryTextShouldCreateQueryWithoutOrdering_whenOrderingIsNotPresent() {
        CosmosStoreQuery query = mock(CosmosStoreQuery.class);
        when(query.getSort()).thenReturn(Sort.unsorted());

        String queryText = "Select id, f1, f2, f3";
        String expectedQuery = "Select id, f1, f2, f3 ";

        SqlQuerySpec querySpec = findQuerySpecGenerator.generateWithQueryText(query, queryText);

        assertEquals(expectedQuery, querySpec.getQueryText());
    }

    @Test
    void generateWithQueryTextShouldThrowIllegalArgumentsException_whenIgnoreCaseIsPresentOnSort() {
        //arrange
        CosmosStoreQuery query = mock(CosmosStoreQuery.class);
        when(query.getSort()).thenReturn(Sort.by(Sort.Order.by("id").ignoreCase()));

        //act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> findQuerySpecGenerator.generateWithQueryText(query, "queryText"));

        assertTrue(exception.getMessage().contains("Ignore case is not supported"));
    }

    @Test
    void generateShouldGenerateQuery_whenCosmosQueryIsProvided() {
        CosmosStoreQuery query = mock(CosmosStoreQuery.class);
        when(query.getSort()).thenReturn(Sort.by("id"));
        String expectedQuery = "SELECT * FROM c ORDER BY c.id ASC";

        SqlQuerySpec sqlQuerySpec = findQuerySpecGenerator.generate(query);

        assertEquals(expectedQuery, sqlQuerySpec.getQueryText());
    }

    @Test
    void generateWithQueryTextShouldIgnoreOrder_whenOrderingIsNotPresent() {
        CosmosStoreQuery query = new CosmosStoreQuery();
        query.with(mock(Pageable.class));
        query.with(Sort.by("id")).with(Sort.unsorted());

        String queryText = "Select id, f1, f2, f3 from c";
        String expectedQuery = "Select id, f1, f2, f3 from c ORDER BY c.id ASC";

        SqlQuerySpec querySpec = findQuerySpecGenerator.generateWithQueryText(query, queryText);
        assertEquals(expectedQuery, querySpec.getQueryText());
    }
}
