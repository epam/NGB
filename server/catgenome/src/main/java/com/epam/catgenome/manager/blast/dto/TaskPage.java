package com.epam.catgenome.manager.blast.dto;

import com.epam.catgenome.entity.blast.Task;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TaskPage {
    List<Task> tasks;
    long totalCount;
}
