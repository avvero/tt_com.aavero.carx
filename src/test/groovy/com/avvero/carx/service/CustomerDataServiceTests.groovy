package com.avvero.carx.service

import com.avvero.carx.App
import com.avvero.carx.conf.LocationRepositoryTestConfiguration
import com.avvero.carx.constants.CommonConstants
import com.avvero.carx.dao.jpa.CustomerRepository
import com.avvero.carx.dao.mongo.CustomerDataRepository
import org.apache.camel.ProducerTemplate
import org.bson.Document
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.SpringApplicationContextLoader
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import spock.lang.Unroll


/**
 * Tests for CustomerDataService
 * @author Avvero
 */
@ContextConfiguration(classes = [App, LocationRepositoryTestConfiguration], loader = SpringApplicationContextLoader)
@ActiveProfiles("test")
class CustomerDataServiceTests extends Specification {

    @Autowired
    CustomerDataService customerDataService;
    @Autowired
    CustomerDataRepository customerDataRepository;
    @Autowired
    CustomerRepository customerRepository;
    @Autowired
    ProducerTemplate producerTemplate;

    def cleanup() {
        customerRepository.deleteAll()
        customerDataRepository.deleteAll()
    }

    @Unroll
    def "CustomerData can be inserted or updated"() {
        setup:
            customerDataService.updateCustomerData("aaaaaa", new Document([money: 100, country: "RUS"]))
        when:
            customerDataService.updateCustomerData(uuid, new Document(json))
            def fetchedJson = customerDataService.findOneCustomerDataByUuid(uuid)
        then:
            fetchedJson.money == json.money
            fetchedJson.country == json.country
            customerRepository.findAll().country == countriesOfStored

            customerRepository.count() == customerCount
            customerDataRepository.count() == storedDataCount
        where:
            uuid     | json                         | customerCount | storedDataCount | countriesOfStored
            "aaaaaa" | [money: 200, country: "RUS"] | 1             | 1               | ['RUS']
            "bbbbbb" | [money: 300, country: "GBR"] | 2             | 2               | ['RUS', 'GBR']
    }

    @Unroll
    def "Existed CustomerData can be fetched by customer uuid"() {
        when:
            customerDataService.updateCustomerData("aaaaaa", new Document([money: 100, country: "RUS"]))
        then:
            customerDataService.findOneCustomerDataByUuid(uuid) == result
        where:
            uuid     | result
            "aaaaaa" | [money: 100, country: "RUS"]
            "bbbbbb" | null
    }

    @Unroll
    def "TODO Max money by country"() {
        setup:
            customerDataService.updateCustomerData("a1", new Document([money: 100, country: "RUS"]))
            customerDataService.updateCustomerData("a2", new Document([money: 200, country: "RUS"]))
            customerDataService.updateCustomerData("a3", new Document([money: 300, country: "RUS"]))
            customerDataService.updateCustomerData("b1", new Document([money: 100, country: "GBR"]))
            customerDataService.updateCustomerData("c1", new Document([money: 100, country: "GBR"]))
            customerDataService.updateCustomerData("c2", new Document([money: 200, country: "GBR"]))
        expect:
            1 == 1
//            def result = customerDataRepository.findTopByMoney()

    }

    def "Can update a lot of data"() {
        when:
            1000.times {
                customerDataService.updateCustomerData("a_${it}", new Document([money: 1, country: "RUS"]))
            }
        then:
            noExceptionThrown()
    }

    def "Can update a lot of data async"() {
        when:
            1000.times {
                producerTemplate.sendBodyAndHeader("direct:customer-data-update",
                        new Document([money: 1, country: "RUS"]), CommonConstants.UUID, "a_${it}");
            }
        then:
            noExceptionThrown()
    }

}
