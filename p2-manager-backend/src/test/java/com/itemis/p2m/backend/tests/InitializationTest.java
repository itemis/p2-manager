package com.itemis.p2m.backend.tests;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.itemis.p2m.backend.InstallableUnitController;
import com.itemis.p2m.backend.RepositoryController;

@RunWith(SpringRunner.class)
@SpringBootTest
public class InitializationTest {

	@Autowired
	private RepositoryController repoController;
	
	@Autowired
	private InstallableUnitController unitController;
	
	@Test
	public void contextLoads() {
		assertNotNull("RepositoryController was not initialized!", repoController);
		assertNotNull("InstallableUnitController was not initialized!", unitController);
	}
}
